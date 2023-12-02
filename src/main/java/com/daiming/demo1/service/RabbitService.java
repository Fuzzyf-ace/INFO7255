package com.daiming.demo1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RabbitService {

    private RabbitTemplate rabbitTemplate;
    private Queue queue;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RabbitService(RabbitTemplate rabbitTemplate, Queue queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.queue = queue;
    }

    public void send(String operation, Map<String, Object> object) {
        Map<String, Object> order = new HashMap<>();
        order.put("operation", operation);
        order.put("object", object);
        String orderJson = null;
        try {
             orderJson = objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        rabbitTemplate.convertAndSend(this.queue.getName(), orderJson);
    }


}
