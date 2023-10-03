package com.daiming.demo1.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class Plan extends JsonDataModel implements Serializable {
    private String planType;
    private MemberCostShare planCostShares;
    private PlanService[] linkedPlanServices;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date creationDate;
}
