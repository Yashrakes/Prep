package com.ParkingLot.ParkingLot.parking;

import java.util.List;

public class NaturalOrderParking extends ParkingStrategy{
    @Override
    public ParkingSpace park(List<ParkingSpace> availableSpaces) {
        if(!availableSpaces.isEmpty()){
            return availableSpaces.get(0);
        }
        return null;
    }
}
