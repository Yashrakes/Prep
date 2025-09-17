package com.Uber.Strategies;

import com.Uber.Interfaces.PricingStrategy;
import com.Uber.Models.TripMetadata;

public class RatingBasedPricingStrategy implements PricingStrategy {
    @Override
    public double calclateFair(TripMetadata metadata) {
        return 10.11;
    }
}
