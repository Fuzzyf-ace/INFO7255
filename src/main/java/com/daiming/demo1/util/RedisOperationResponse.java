package com.daiming.demo1.util;

import lombok.Getter;

@Getter
public enum RedisOperationResponse {
    SUCCESS("success"),
    KEY_NOT_FOUND("key not found"),
    KEY_EXISTS("key exists"),
    FAILURE("failure");

    private final String value;

    RedisOperationResponse(String value) {
        this.value = value;
    }

}
