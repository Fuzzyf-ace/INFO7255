package com.daiming.demo1.model.dto;

import com.daiming.demo1.model.MemberCostShare;
import com.daiming.demo1.model.PlanService;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Data
public class UpdatePlanPatchRequestBody {
    private String planType;
    private MemberCostShare planCostShares;
    private UpdatePlanServiceDTO linkedPlanServices;
    private String creationDate;
}
