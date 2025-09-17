package com.HLDLLD.behavioural.Strategy.Vehic;

import com.HLDLLD.behavioural.Strategy.Drivee.DriveStrategy;

public class Vehicle {

    DriveStrategy driveStrategy;
    Vehicle(DriveStrategy driveStrategy){
        this.driveStrategy=driveStrategy;
    }
    public void drive(){
        driveStrategy.drive();
    }
}
