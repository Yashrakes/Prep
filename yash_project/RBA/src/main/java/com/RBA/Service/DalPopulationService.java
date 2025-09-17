package com.RBA.Service;

import com.RBA.Model.CreditDecisionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.apache.coyote.Response;
import org.springframework.stereotype.Service;

import javax.management.Query;

@Service
public class DalPopulationService {

    @Value("${spring.application.name}")
    private String name;

    public void populate(CreditDecisionRequest creditDecisionRequest){
        Integer partyId = Math.toIntExact(creditDecisionRequest.getLimits().get(0).getLimitId());
        if(partyId!=null){
            getPartyDetails(partyId, creditDecisionRequest.getEmail(), creditDecisionRequest);
        }
    }

    private void getPartyDetails(Integer partyId, String userEmail, CreditDecisionRequest creditDecisionRequest){
        // Business Logic Come Here...
        String url = "http://global/graphql";
        Query query;// = Query.builder(url).userName(userEmail).appConsumerName(name);
        String criteria = "1101";
        Response res;// = query.bulkParty(query,criteria, new Filter());
        creditDecisionRequest.getLimits().get(0).setLiveDetails(null);
        creditDecisionRequest.getLimits().get(0).setProposedDetails(null);
    }
}
