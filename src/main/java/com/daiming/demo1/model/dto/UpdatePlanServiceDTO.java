package com.daiming.demo1.model.dto;

import com.daiming.demo1.model.PlanService;
import lombok.Data;

import java.util.Optional;

@Data
public class UpdatePlanServiceDTO {
    private PlanService[] add;
    private PlanService[] delete;
    private PlanService[] update;
}
