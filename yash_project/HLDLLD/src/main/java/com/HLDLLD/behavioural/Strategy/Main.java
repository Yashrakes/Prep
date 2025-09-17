package com.HLDLLD.behavioural.Strategy;


import com.HLDLLD.behavioural.Strategy.Vehic.GoodsVehicle;
import com.HLDLLD.behavioural.Strategy.Vehic.SportVehicle;
import com.HLDLLD.behavioural.Strategy.Vehic.Vehicle;

public class Main {
    public static void main(String args[]){
        Vehicle vehicle = new SportVehicle();
        Vehicle vehicle1 = new GoodsVehicle();

        vehicle.drive();
        vehicle1.drive();
    }
}
