package com.daiming.demo1.util;

public class RedisKeyGenerator {

    public static String generateKey(String objectId, String objectType) {
        return String.format("%s:%s", objectType, objectId);
    }
}
