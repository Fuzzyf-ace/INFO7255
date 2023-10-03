package com.daiming.demo1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class MemberCostShare extends JsonDataModel implements Serializable {
    private int deductible;
    private int copay;
}
