package com.daiming.demo1.service;

import com.daiming.demo1.model.LinkedService;
import com.daiming.demo1.model.MemberCostShare;
import com.daiming.demo1.model.Plan;
import com.daiming.demo1.model.PlanService;
import com.daiming.demo1.model.dto.UpdatePlanPatchRequestBody;
import com.daiming.demo1.model.dto.UpdatePlanServiceDTO;
import com.daiming.demo1.util.ETagGenerator;
import com.daiming.demo1.util.RedisOperationResponse;
import com.nimbusds.jose.util.ArrayUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@Service
public class RedisService {

    private final RedisTemplate redisTemplate;

    public RedisService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RedisOperationResponse createPlan(Plan planEntity) {
        if (redisTemplate.opsForValue().get(planEntity.getObjectId()) != null) {
            return RedisOperationResponse.KEY_EXISTS;
        }
        String objectType = planEntity.getObjectType();
        String objectId = planEntity.getObjectId();
        // 存储整个对象
        redisTemplate.opsForValue().set(objectType + ":" + objectId + ":Plan", planEntity);

        // 存储 MemberCostShare
        MemberCostShare planCostShares = planEntity.getPlanCostShares();

        redisTemplate.opsForValue().set(objectType + ":" + objectId + ":MemberCostShare:" + planCostShares.getObjectId(), planCostShares);

        // 对于 linkedPlanServices 数组的处理
        PlanService[] linkedPlanServices = planEntity.getLinkedPlanServices();
        for (PlanService planService : linkedPlanServices) {
            // 存储 Service
            redisTemplate.opsForValue().set(planService.getObjectId() + ":" + objectId + ":PlanService:" + planService.getObjectId(), planService);
            // 对于 MemberCostShare 数组的处理
            MemberCostShare memberCostShare = planService.getPlanserviceCostShares();
            redisTemplate.opsForValue().set(objectType + ":" + objectId + ":PlanService:" + planService.getObjectId() + ":MemberCostShare:" + memberCostShare.getObjectId(), memberCostShare);
            LinkedService linkedService = planService.getLinkedService();
            redisTemplate.opsForValue().set(objectType + ":" + objectId + ":PlanService:" + planService.getObjectId() + ":LinkedService:" + linkedService.getObjectId(), linkedService);
        }
        return RedisOperationResponse.SUCCESS;
    }


    public Plan getPlan(String id) {
        return (Plan) redisTemplate.opsForValue().get(id);
    }

    public RedisOperationResponse deletePlan(String id, String etag) {
        Plan plan = (Plan) redisTemplate.opsForValue().get(id);
        if (plan == null) {
            return RedisOperationResponse.KEY_NOT_FOUND;
        }
        //check etag if match
        if (etag != null) {
            String etagFromRedis = ETagGenerator.generateETag(plan);
            if (!etagFromRedis.equals(etag)) {
                return RedisOperationResponse.FAILURE;
            }
        }
        boolean success = Boolean.TRUE.equals(redisTemplate.delete(id));
        if (success) {
            return RedisOperationResponse.SUCCESS;
        } else {
            return RedisOperationResponse.FAILURE;
        }
    }

    public Plan updatePlan(Plan plan, UpdatePlanPatchRequestBody requestBody) {

        if (requestBody.getPlanType() != null) {
            plan.setPlanType(requestBody.getPlanType());
        }
        if (requestBody.getPlanCostShares() != null) {
            plan.setPlanCostShares(requestBody.getPlanCostShares());
        }
        if (requestBody.getLinkedPlanServices() != null) {
            UpdatePlanServiceDTO updatePlanServiceDTO = requestBody.getLinkedPlanServices();
            if (updatePlanServiceDTO.getAdd() != null) {
                PlanService[] addPlanServices = updatePlanServiceDTO.getAdd();
                PlanService[] originalPlanServices = plan.getLinkedPlanServices();
                PlanService[] newPlanServices = Stream.concat(Arrays.stream(originalPlanServices), Arrays.stream(addPlanServices)).distinct().toArray(PlanService[]::new);
                plan.setLinkedPlanServices(newPlanServices);
            }
            if (updatePlanServiceDTO.getDelete() != null) {
                PlanService[] deletePlanServices = updatePlanServiceDTO.getDelete();
                HashSet<PlanService> deletePlanServicesSet = new HashSet<>(Arrays.asList(deletePlanServices));
                PlanService[] originalPlanServices = plan.getLinkedPlanServices();
                PlanService[] newPlanServices = Arrays.stream(originalPlanServices).filter(planService -> !deletePlanServicesSet.contains(planService)).toArray(PlanService[]::new);
                plan.setLinkedPlanServices(newPlanServices);
            }
            if (updatePlanServiceDTO.getUpdate() != null) {
                PlanService[] updatePlanServices = updatePlanServiceDTO.getUpdate();
                HashMap<String, PlanService> updatePlanServicesSet = Arrays.stream(updatePlanServices).collect(HashMap::new, (m, v) -> m.put(v.getObjectId(), v), HashMap::putAll);
                PlanService[] originalPlanServices = plan.getLinkedPlanServices();
                PlanService[] newPlanServices = Arrays.stream(originalPlanServices).map(planService ->
                        updatePlanServicesSet.getOrDefault(planService.getObjectId(), planService)
                ).toArray(PlanService[]::new);
                plan.setLinkedPlanServices(newPlanServices);
            }
        }
        if (requestBody.getCreationDate() != null) {
            plan.setCreationDate(requestBody.getCreationDate());
        }
        redisTemplate.opsForValue().set(plan.getObjectId(), plan);
        return plan;
    }
}
