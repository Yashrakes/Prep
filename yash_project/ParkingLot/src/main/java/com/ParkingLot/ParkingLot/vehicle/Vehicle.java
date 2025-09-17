package com.ParkingLot.ParkingLot.vehicle;

import lombok.Data;

@Data
public abstract class Vehicle{
    VehicleType vehicleType;
    String regNum;

    public Vehicle(String regNum, VehicleType vehicleType){
        this.regNum = regNum;
        this.vehicleType = vehicleType;
    }

}
