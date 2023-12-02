package com.daiming.demo1.service;

import com.daiming.demo1.model.Plan;
import com.daiming.demo1.model.PlanService;
import com.daiming.demo1.model.dto.UpdatePlanPatchRequestBody;
import com.daiming.demo1.model.dto.UpdatePlanServiceDTO;
import com.daiming.demo1.util.ETagGenerator;
import com.daiming.demo1.util.RedisOperationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import org.json.JSONObject;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.json.JSONArray;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final RabbitService rabbitService;

    public RedisService(StringRedisTemplate redisTemplate,
                        RabbitService rabbitService) {
        this.redisTemplate = redisTemplate;
        this.rabbitService = rabbitService;
        this.objectMapper = new ObjectMapper();
    }

    private List<Map<String, Map<String, Object>>> saveJSONArrayToRedis(JSONArray array, String parrentId, String name) {
        List<Map<String, Map<String, Object>>> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                //code never reach here
                System.out.println("array");
                List<Map<String, Map<String, Object>>> convertedValue = saveJSONArrayToRedis((JSONArray) value, parrentId, name);
                list.addAll(convertedValue);
            } else if (value instanceof JSONObject) {
                JSONObject object = (JSONObject) value;
                Map<String, Map<String, Object>> convertedValue = saveJSONObjectToRedis(object, parrentId, name);
                list.add(convertedValue);
            }
        }
        return list;
    }

    public Map<String, Map<String, Object>> saveJSONObjectToRedis(JSONObject object, String parentId, String name) {
        Map<String, Map<String, Object>> redisKeyMap = new HashMap<>();
        Map<String, Object> objectFieldMap = new HashMap<>();
        String redisKey = object.get("objectType") + ":" + object.get("objectId");
        String id = (String) object.get("objectId");
        for (String field : object.keySet()) {
            Object value = object.get(field);
            if (value instanceof JSONObject) {
                Map<String, Map<String, Object>> convertedValue = saveJSONObjectToRedis((JSONObject) value, id, field);
                redisTemplate.opsForSet().add(redisKey + ":" + field,
                        convertedValue.entrySet().iterator().next().getKey());
            } else if (value instanceof JSONArray) {
                List<Map<String, Map<String, Object>>> convertedValue = saveJSONArrayToRedis((JSONArray) value, id, field);
                for (Map<String, Map<String, Object>> entry : convertedValue) {
                    for (String listKey : entry.keySet()) {
                        redisTemplate.opsForSet().add(redisKey + ":" + field, listKey);
                    }
                }
            } else {
                redisTemplate.opsForHash().put(redisKey, field, value.toString());
                objectFieldMap.put(field, value);
                redisKeyMap.put(redisKey, objectFieldMap);
            }
        }

        HashMap<String, Object> planJoin = new HashMap<>();
        planJoin.put("name", name);
        planJoin.put("parent", parentId);

        objectFieldMap.put("plan_join", planJoin);

        rabbitService.send("create", objectFieldMap);
        return redisKeyMap;
    }


    public RedisOperationResponse createPlan(Plan planEntity) {
        if (!redisTemplate.keys("plan:" + planEntity.getObjectId() + "*").isEmpty()) {
            return RedisOperationResponse.KEY_EXISTS;
        }
        //transfer planEntity to JSONObject
        JSONObject object = new JSONObject(planEntity);
        saveJSONObjectToRedis(object, "root", "plan");
        return RedisOperationResponse.SUCCESS;
    }


    public Map<String, Object> getObject(String objectKey) {
        HashOperations<String, String, Object> operations = redisTemplate.opsForHash();
        Set<String> keys = redisTemplate.keys(objectKey + ":*");
//        keys.add(objectKey);
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> object = operations.entries(objectKey);
        for (String field : object.keySet()) {
            resultMap.put(field, object.get(field));
        }
        for (String key : keys) {
            Set<String> childrenKeys = redisTemplate.opsForSet().members(key);
            List<Map<String, Object>> children = new ArrayList<>();
            for (String childKey : childrenKeys) {
                Map<String, Object> subResult = getObject(childKey);
                children.add(subResult);
            }
            if (children.size() > 1) {
                resultMap.put(key.split(":")[2], children);
            } else {
                resultMap.put(key.split(":")[2], children.get(0));
            }

        }
        return resultMap;
    }

    public Plan getPlan(String id) {
        String redisKey = "plan:" + id;
        Map<String, Object> objectMap = getObject(redisKey);
        if (objectMap.isEmpty()) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Plan plan = null;
        try {
            String jsonString = objectMapper.writeValueAsString(objectMap);
            JsonNode planNode = JsonLoader.fromString(jsonString);
            plan = objectMapper.treeToValue(planNode, Plan.class);
        } catch (IOException e) {
            return null;
        }
        return plan;
    }

    public RedisOperationResponse deletePlan(String id, String etag) {
        Plan plan = getPlan(id);
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
        boolean success = deleteObject("plan:" + id);
        if (success) {
            return RedisOperationResponse.SUCCESS;
        } else {
            return RedisOperationResponse.FAILURE;
        }
    }

    public boolean deleteObject(String objectKey) {
        Set<String> keys = redisTemplate.keys(objectKey + "*");
        try {
            for (String key : keys) {
                if (!key.equals(objectKey)) {
                    Set<String> childrenKeys = redisTemplate.opsForSet().members(key);
                    for (String childKey : childrenKeys) {
                        deleteObject(childKey);
                    }
                }
                redisTemplate.delete(key);
                if (key.equals(objectKey)) {
                    String id = key.split(":")[1];
                    rabbitService.send("delete", Map.of("objectId", id));
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
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
        deletePlan(plan.getObjectId(), null);
        createPlan(plan);
        Plan updatedPlan = getPlan(plan.getObjectId());
        return updatedPlan;
    }

    public boolean hasKey(String redisKey) {
        return redisTemplate.hasKey(redisKey);
    }

}
