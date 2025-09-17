package com.RBA.Model;

import lombok.Data;

import java.util.List;

@Data
public class CreditDecisionResponse {

    private String overallOutcome;
    private List<RuleResponse> rules;
}
