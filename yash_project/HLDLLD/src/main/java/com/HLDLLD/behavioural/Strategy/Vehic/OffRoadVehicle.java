package com.HLDLLD.behavioural.Strategy.Vehic;

import com.HLDLLD.behavioural.Strategy.Drivee.DriveStrategy;


public class OffRoadVehicle extends Vehicle {
    //DriveStrategy nordrive = new NorDrive();

    public OffRoadVehicle(DriveStrategy nordrive){
        super(nordrive);
    }
}
