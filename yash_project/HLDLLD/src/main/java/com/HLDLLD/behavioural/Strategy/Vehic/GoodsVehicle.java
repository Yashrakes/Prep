package com.HLDLLD.behavioural.Strategy.Vehic;

import com.HLDLLD.behavioural.Strategy.Drivee.NorDrive;


public class GoodsVehicle extends Vehicle {

    public GoodsVehicle(){
        super(new NorDrive());
    }
}
