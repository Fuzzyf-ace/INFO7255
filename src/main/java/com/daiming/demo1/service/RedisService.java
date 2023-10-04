package com.daiming.demo1.service;

import com.daiming.demo1.model.Plan;
import com.daiming.demo1.util.RedisOperationResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
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

    public RedisOperationResponse deletePlan(String id) {
        Plan plan = (Plan) redisTemplate.opsForValue().get(id);
        if (plan == null) {
            return RedisOperationResponse.KEY_NOT_FOUND;
        }
        boolean success = Boolean.TRUE.equals(redisTemplate.delete(id));
        if (success) {
            return RedisOperationResponse.SUCCESS;
        } else {
            return RedisOperationResponse.FAILURE;
        }
    }
}
