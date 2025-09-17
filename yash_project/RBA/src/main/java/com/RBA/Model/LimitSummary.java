package com.RBA.Model;

import lombok.Data;

@Data
public class LimitSummary {
    private String limitId;
    private CreditDecisionRequest creditDecisionRequestForLimit;
    private RuleList rulesSummaryForLimit;
    private CreditDecisionResponse creditDecisionResponse;
}
