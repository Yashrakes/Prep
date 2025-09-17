package com.ParkingLot.ParkingLot.parking;

import java.util.List;

public abstract class ParkingStrategy {

    public abstract ParkingSpace park(List<ParkingSpace> availableSpaces);
}
