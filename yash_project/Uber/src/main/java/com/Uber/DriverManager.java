package com.Uber;

import com.Uber.Models.Driver;

import java.util.HashMap;

public class DriverManager {
    private static DriverManager driverManager;

    HashMap<String, Driver> driversMap;

    // Private constructor to prevent instantiation
    private DriverManager() {
        // Initialization logic (e.g., loading DB driver)
        driversMap = new HashMap<>();
        System.out.println("DriverManager instance created");
    }

    public static synchronized DriverManager getInstance(){
        if(driverManager==null){
            synchronized (DriverManager.class){
                if(driverManager == null){
                    driverManager = new DriverManager();
                }

            }

        }
        return driverManager;
    }

    public void addDriver(String driverName, Driver driver){
        driversMap.put(driverName, driver);
    }

    public Driver getDriver(String driverName){
        return driversMap.get(driverName);
    }

    public HashMap<String, Driver> getDriversMap(){
        return driversMap;
    }
}
