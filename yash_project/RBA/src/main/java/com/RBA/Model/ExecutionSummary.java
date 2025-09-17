package com.RBA.Model;

import lombok.Data;

@Data
public abstract class ExecutionSummary {
    private final Rule rule;
    private final String evaluationResult; // True, False, Error

    protected ExecutionSummary(Rule rule, String evaluationResult) {
        this.rule = rule;
        this.evaluationResult = evaluationResult;
    }
}
