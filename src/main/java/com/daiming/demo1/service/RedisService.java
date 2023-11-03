package com.daiming.demo1.service;

import com.daiming.demo1.model.Plan;
import com.daiming.demo1.util.ETagGenerator;
import com.daiming.demo1.util.RedisOperationResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
}
