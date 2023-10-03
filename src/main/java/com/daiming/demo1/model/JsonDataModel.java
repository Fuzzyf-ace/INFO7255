package com.daiming.demo1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
abstract public class JsonDataModel implements Serializable {
    @JsonProperty("_org")
    private String org;
    private String objectId;
    private String objectType;
}
