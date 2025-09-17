package com.Uber.Strategies;

import com.Uber.DriverManager;
import com.Uber.Interfaces.DriverMatchingStrategy;
import com.Uber.Models.Driver;
import com.Uber.Models.TripMetadata;

import java.util.HashMap;

public class LeastTimeBasedDriverMatchingStrategy implements DriverMatchingStrategy {
    @Override
    public Driver findDriver(TripMetadata tripMetadata) {
        DriverManager driverMgr = DriverManager.getInstance();

        HashMap<String, Driver> driversMap =  driverMgr.getDriversMap();
        return driversMap.get("yogita");
    }
}
