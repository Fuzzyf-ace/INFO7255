package com.daiming.demo1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlanService extends JsonDataModel implements Serializable {
    private Service linkedService;
    private MemberCostShare planserviceCostShares;
}
