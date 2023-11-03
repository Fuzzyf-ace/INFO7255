package com.daiming.demo1.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public class ETagGenerator {
    public static String generateETag(Object object) {
//        boolean isWeak = false;
        String content = toJsonString(object);
        StringBuilder builder = new StringBuilder(37);
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
//        if (isWeak) {
//            builder.append("W/");
//        }
        builder.append("\"0");
        try {
            DigestUtils.appendMd5DigestAsHex(inputStream, builder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        builder.append('"');
        return builder.toString();
    }

    public static String toJsonString(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return "";
        }
    }
}
