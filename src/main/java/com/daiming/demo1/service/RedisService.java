package com.daiming.demo1.service;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
        redisTemplate.opsForValue().set(planEntity.getObjectId(), planEntity);
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
