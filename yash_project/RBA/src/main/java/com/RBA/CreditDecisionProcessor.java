package com.RBA;

import com.RBA.Model.CreditDecisionRequest;
import com.RBA.Model.CreditDecisionResponse;

public interface CreditDecisionProcessor {

    CreditDecisionResponse executeRules(CreditDecisionRequest creditDecisionRequest);
}