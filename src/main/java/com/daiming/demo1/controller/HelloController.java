package com.daiming.demo1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/hello")
    public String hello() {
        return "hello world";
    }

    @PostMapping("/name")
    @ResponseStatus(code = org.springframework.http.HttpStatus.CREATED, reason = "Created")
    public String setName(@RequestBody String name) {
        redisTemplate.opsForValue().set("name", name);
        return "success";
    }
    @GetMapping("/name")
    public String getName() {
        return (String) redisTemplate.opsForValue().get("name");
    }


}
