package com.Uber;

import com.Uber.Interfaces.DriverMatchingStrategy;
import com.Uber.Interfaces.PricingStrategy;
import com.Uber.Models.*;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TripManager {
    private AtomicInteger count = new AtomicInteger(0);
    public  static  volatile TripManager instance;
    private RiderManager riderManager;
    private DriverManager driverManager;
    HashMap<Integer, TripMetadata> tripsMetadataInfo;
    HashMap<Integer, Trip> tripsInfo;

    private TripManager(){
        riderManager = RiderManager.getInstance();
        driverManager = DriverManager.getInstance();
        tripsInfo = new HashMap<>();
        tripsMetadataInfo = new HashMap<>();
    }

    public static TripManager getInstance(){
        if(instance==null){
            synchronized (TripManager.class){
                if(instance==null){
                    instance = new TripManager();
                }
            }
        }
        return instance;
    }

    public HashMap<Integer, Trip> getTripsMap(){
        return tripsInfo;
    }


    public void createTrip(Rider rider, Location src, Location dest){
        TripMetadata tripMetadata = new TripMetadata(src, dest, rider.getRating());
        StrategyManager strategyManager = StrategyManager.getInstance();
        PricingStrategy pricingStrategy = strategyManager.determinePricingStrategy(tripMetadata);
        DriverMatchingStrategy driverMatchingStrategy = strategyManager.determineDriverMatchingStrategy(tripMetadata);

        Driver driver = driverMatchingStrategy.findDriver(tripMetadata);
        double tripPrice = pricingStrategy.calclateFair(tripMetadata);
        Trip trip = new Trip();
        trip.setRider(rider);
        trip.setDriver(driver);
        trip.setPrice(tripPrice);
        trip.setTripId(count.incrementAndGet());
        int tripId = trip.getTripId();
        tripsInfo.put(tripId,trip);
        tripsMetadataInfo.put(tripId,tripMetadata);

    }

}
