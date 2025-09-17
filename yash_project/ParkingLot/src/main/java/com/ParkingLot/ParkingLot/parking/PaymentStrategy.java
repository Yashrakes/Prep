package com.ParkingLot.ParkingLot.parking;

import com.ParkingLot.ParkingLot.vehicle.Vehicle;
import lombok.Data;

@Data
public abstract class PaymentStrategy {
    private double bikeCharges;
    private double carCharges;
    private double truckCharges;
    public abstract double calculateCost(Ticket t);
    protected double getChargeType(Vehicle v){
        switch (v.getVehicleType()){
            case CAR :
                return carCharges;
            case BIKE:
                return bikeCharges;
            case TRUCK:
                return truckCharges;
        }
        return 0;
    }
}
