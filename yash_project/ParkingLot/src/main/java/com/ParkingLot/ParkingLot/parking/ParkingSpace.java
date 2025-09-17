package com.ParkingLot.ParkingLot.parking;

import com.ParkingLot.ParkingLot.vehicle.Vehicle;
import lombok.Data;

@Data
public abstract class ParkingSpace {
    Vehicle vehicle;
    ParkingSpaceType parkingSpaceType;
    String spaceId;
    boolean isEmpty;

    public ParkingSpace(String spaceId, ParkingSpaceType type){
        this.spaceId= spaceId;
        this.parkingSpaceType= type;
        this.isEmpty= true;
    }
    public void parkVehicle(Vehicle v)
    {
        isEmpty= false;
        vehicle= v;
    }
    public void removeVehicle()
    {
        vehicle= null;
        isEmpty= true;
    }
}
