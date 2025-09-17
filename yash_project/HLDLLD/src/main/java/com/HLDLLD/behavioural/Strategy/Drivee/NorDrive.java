package com.HLDLLD.behavioural.Strategy.Drivee;


import com.HLDLLD.behavioural.Strategy.Drivee.DriveStrategy;

public class NorDrive implements DriveStrategy {
    @Override
    public void drive(){
        System.out.println("normal drive");
    }
}
