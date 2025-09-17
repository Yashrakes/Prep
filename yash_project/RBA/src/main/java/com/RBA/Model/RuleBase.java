package com.RBA.Model;

import lombok.Data;

@Data
public class RuleBase {
    private long ruleId;
    private String ruleType;
    private String ruleName;
    private String ruleShortName;
    private String ruleExpression;
    private String ruleDescription;
}
