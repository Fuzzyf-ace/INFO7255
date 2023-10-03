package com.daiming.demo1.controller;

import com.daiming.demo1.model.Plan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController()
public class PlanController {

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/plan")
    public ResponseEntity<String> createPlan(@RequestBody String plan) throws IOException, ProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode schemaNode = JsonLoader.fromResource("/jsonSchema.json");
        JsonSchema jsonSchema = JsonSchemaFactory.byDefault().getJsonSchema(schemaNode);
        JsonNode planNode = JsonLoader.fromString(plan);
        Plan planEntity = objectMapper.treeToValue(planNode, Plan.class);
        ProcessingReport validate = jsonSchema.validate(planNode);
        if (validate.isSuccess()) {
            redisTemplate.opsForValue().set(planEntity.getObjectId(), planEntity);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    objectMapper.createObjectNode().put("message", "Created")
                            .put("objectId", planEntity.getObjectId()).toString()
            );
        } else {
            return ResponseEntity.badRequest().body(validate.toString());
        }
    }

    @GetMapping("/plan/{id}")
    public ResponseEntity<Plan> getPlan(@PathVariable String id) {
        return ResponseEntity.ok((Plan) redisTemplate.opsForValue().get(id));
    }

    @DeleteMapping("/plan/{id}")
    public ResponseEntity<String> deletePlan(@PathVariable String id) {
        redisTemplate.delete(id);
        return ResponseEntity.ok("Deleted");
    }
}
