package com.RBA.Model;

import lombok.Data;

import java.util.List;

@Data
public class RuleList {

    private String limitType;
    private List<Rule> differentiatorRules;
    private List<Rule> incresingRules;
    private List<Rule> decreasingRules;
    private List<ExecutionSummary> executionSummaryIncrease;
    private List<ExecutionSummary> executionSummaryDecrease;
}
