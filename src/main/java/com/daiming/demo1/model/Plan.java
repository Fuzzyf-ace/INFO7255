package com.daiming.demo1.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class Plan extends JsonDataModel implements Serializable {
    private String planType;
    private MemberCostShare planCostShares;
    private PlanService[] linkedPlanServices;
    private String creationDate;

    public static Plan emptyPlan() {
        Plan plan = new Plan();
        plan.setPlanType(null);
        plan.setPlanCostShares(null);
        plan.setLinkedPlanServices(null);
        plan.setCreationDate(null);
        return plan;
    }
}
