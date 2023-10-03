package com.daiming.demo1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class Service extends JsonDataModel implements Serializable {
    private String name;
}
