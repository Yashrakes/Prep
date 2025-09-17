package com.RBA.Model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Limit {

    private long limitId;
    private BigDecimal limitAmount;
    private Integer maturityDays;
    private String counterPartyDetails;
    private String productDetails;
    private LocalDate ratingReviewDate;
    private String glcDetails;
    private String liveDetails;
    private String proposedDetails;
}
