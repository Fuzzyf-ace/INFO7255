package com.daiming.demo1.model;

import lombok.Data;

import java.io.Serializable;

@Data
abstract public class JsonDataModel implements Serializable {
    private String _org;
    private String objectId;
    private String objectType;
}
