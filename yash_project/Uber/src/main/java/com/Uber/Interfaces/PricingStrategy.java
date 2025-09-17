package com.Uber.Interfaces;

import com.Uber.Models.TripMetadata;

public interface PricingStrategy {
    public double calclateFair(TripMetadata metadata);
}
