package com.Uber.Interfaces;

import com.Uber.Models.Driver;
import com.Uber.Models.TripMetadata;

public interface DriverMatchingStrategy {
    public Driver findDriver(TripMetadata tripMetadata);
}
