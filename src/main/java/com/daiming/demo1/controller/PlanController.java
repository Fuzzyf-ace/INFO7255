package com.daiming.demo1.controller;

import com.daiming.demo1.model.Plan;
import com.daiming.demo1.service.JsonSchemaValidateService;
import com.daiming.demo1.service.RedisService;
import com.daiming.demo1.util.RedisOperationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class PlanController {


    private final JsonSchemaValidateService jsonSchemaValidateService;

    private final RedisService redisService;

    public PlanController(JsonSchemaValidateService jsonSchemaValidateService, RedisService redisService) {
        this.jsonSchemaValidateService = jsonSchemaValidateService;
        this.redisService = redisService;
    }

    @PostMapping("/plan")
    public ResponseEntity<String> createPlan(@RequestBody String plan) throws ProcessingException, IOException {
        JsonNode schemaNode = JsonLoader.fromResource("/jsonSchema.json");
        JsonNode planNode = JsonLoader.fromString(plan);
        ProcessingReport processingReport = jsonSchemaValidateService.validate(planNode, schemaNode);
        if (!processingReport.isSuccess()) return ResponseEntity.badRequest().body(processingReport.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        Plan planEntity = objectMapper.treeToValue(planNode, Plan.class);
        RedisOperationResponse response = redisService.createPlan(planEntity);
        return switch (response) {
            case SUCCESS -> ResponseEntity.status(HttpStatus.CREATED).body(
                    objectMapper.createObjectNode().put("message", "Created")
                            .put("objectId", planEntity.getObjectId()).toString()
            );
            case KEY_EXISTS -> ResponseEntity.status(HttpStatus.CONFLICT).body("ObjectId already exists!!");
            default -> null;
        };
    }

    @GetMapping("/plan/{id}")
    public ResponseEntity<Plan> getPlan(@PathVariable String id) {
        Plan plan = redisService.getPlan(id);
        if (plan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Plan.emptyPlan());
        }
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("/plan/{id}")
    public ResponseEntity<String> deletePlan(@PathVariable String id) {
        RedisOperationResponse response = redisService.deletePlan(id);
        return switch (response) {
            case SUCCESS -> ResponseEntity.noContent().build();
            case KEY_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("ObjectId does not exists!!");
            default -> null;
        };
    }
}
