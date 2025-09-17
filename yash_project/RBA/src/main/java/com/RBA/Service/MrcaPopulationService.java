package com.RBA.Service;

import com.RBA.Model.CreditDecisionRequest;
import org.springframework.stereotype.Service;

@Service
public class MrcaPopulationService {

    public void populate(CreditDecisionRequest creditDecisionRequest){
        //Business Logic Comes Here...
        /*
        private final MrcaServiceImpl mrcaService;
        Set<Integer> parties = mrcaService.getAuthorities(creditDecisionRequest);
         */
        creditDecisionRequest.setAuthorities(null);
    }
}
