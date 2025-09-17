package com.RBA.Model;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Data
public class CreditDecisionRequest {
    private Long requestId;
    private List<Limit> limits;
    private String email;
    private String groupCPTYDetails;
    private String authorities;
}

