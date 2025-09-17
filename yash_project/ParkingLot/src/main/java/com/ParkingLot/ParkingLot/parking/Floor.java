package com.ParkingLot.ParkingLot.parking;

import com.ParkingLot.ParkingLot.vehicle.Vehicle;
import com.ParkingLot.ParkingLot.vehicle.VehicleType;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Floor {
    private final String floorId;
    private Map<ParkingSpaceType, List<ParkingSpace>> pSpaces = new HashMap<>();

    public Floor(String floorId){
        this.floorId =floorId;
        pSpaces.put(ParkingSpaceType.BikeParking, new ArrayList<>());
        pSpaces.put(ParkingSpaceType.CarParking, new ArrayList<>());
        pSpaces.put(ParkingSpaceType.TruckParking, new ArrayList<>());
    }

    public void addParkingSpace(ParkingSpace p) {
        pSpaces.get(p.getParkingSpaceType()).add(p);
    }
    public void removeParkingSpace(ParkingSpace p){
        pSpaces.get(p.getParkingSpaceType()).remove(p);
    }

    public boolean canParkVehicle(VehicleType v) {
        List<ParkingSpace> p = pSpaces.get(getSpaceTypeForVehicle(v));
        for (ParkingSpace pt : p) {
            if (pt.isEmpty) {
                return true;
            }
        }
        return false;
    }
    public ParkingSpaceType getSpaceTypeForVehicle(VehicleType v){
        switch (v){
            case CAR :
                return ParkingSpaceType.CarParking;
            case BIKE :
                return ParkingSpaceType.BikeParking;
            case TRUCK:
                return ParkingSpaceType.TruckParking;
        }
        return null;
    }

    public ParkingSpace getSpace(Vehicle v)
    {
        List<ParkingSpace> availableSpaces= new ArrayList<>();
        for(ParkingSpace p: pSpaces.get(getSpaceTypeForVehicle(v.getVehicleType())))
            if(p.isEmpty())
                availableSpaces.add(p);
        if(availableSpaces.isEmpty()){
            return null;
        }
        return ParkingLot.INSTANCE.getPStrategy().park(availableSpaces);


    }




}
