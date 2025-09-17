package com.HLDLLD.behavioural.Strategy.Vehic;

import com.HLDLLD.behavioural.Strategy.Drivee.SportDrive;

public class SportVehicle extends Vehicle {

    public SportVehicle(){
        super(new SportDrive());
    }

//    public SportVehicle(SportDrive sportDrive){
//        super(sportDrive);
//    }


}
