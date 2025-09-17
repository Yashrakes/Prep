package com.Uber;

import com.Uber.Models.Rider;

import java.util.HashMap;

public class RiderManager {
    private static volatile RiderManager instance;
    private HashMap<String, Rider> ridersMap;

    private RiderManager(){
        this.ridersMap = new HashMap<>();
    }

    public static RiderManager getInstance(){
        if(instance==null){
            synchronized (RiderManager.class){
                if(instance==null){
                    instance = new RiderManager();
                }
            }
        }
        return instance;
    }

    public void addRider(String riderName, Rider rider) {
        ridersMap.put(riderName,rider);
    }

    public Rider getRider(String riderName){
        return ridersMap.get(riderName);
    }

}
