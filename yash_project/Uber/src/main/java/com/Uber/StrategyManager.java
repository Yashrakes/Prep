package com.Uber;

import com.Uber.Interfaces.DriverMatchingStrategy;
import com.Uber.Interfaces.PricingStrategy;
import com.Uber.Models.TripMetadata;
import com.Uber.Strategies.LeastTimeBasedDriverMatchingStrategy;
import com.Uber.Strategies.RatingBasedPricingStrategy;

public class StrategyManager {
    private static volatile StrategyManager instance;

    private StrategyManager(){}

    public static StrategyManager getInstance(){
        if(instance==null){
            synchronized (StrategyManager.class){
                if(instance==null){
                    instance = new StrategyManager();
                }
            }
        }
        return instance;
    }

    public PricingStrategy determinePricingStrategy(TripMetadata tripMetadata){
        return new RatingBasedPricingStrategy();
    }

    public DriverMatchingStrategy determineDriverMatchingStrategy(TripMetadata tripMetadata){
        return new LeastTimeBasedDriverMatchingStrategy();
    }
}
