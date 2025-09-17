package com.ParkingLot.ParkingLot.parking;

import com.ParkingLot.ParkingLot.vehicle.Vehicle;

public class EntryGate {
    private String gateId;

    public EntryGate(String gateId){
        this.gateId= gateId;
    }

    public Ticket generateTicket(Vehicle v){
        if(!ParkingLot.INSTANCE.isParkingSpaceAvailable(v.getVehicleType()))
            return null;

        ParkingSpace pSpace= ParkingLot.INSTANCE.findParkingSpace(v);
        pSpace.parkVehicle(v);
        return new Ticket(v, pSpace);
    }
}
