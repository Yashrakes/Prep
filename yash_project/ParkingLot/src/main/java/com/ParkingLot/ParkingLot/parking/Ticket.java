package com.ParkingLot.ParkingLot.parking;

import com.ParkingLot.ParkingLot.vehicle.Vehicle;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Ticket {
    private final String ticketId;
    private final LocalDateTime entryTime;
    private final Vehicle vehicle;
    private final ParkingSpace pSpaceAssigned;
    private LocalDateTime exitTime;
    private boolean isActive;
    private double charges;

    public Ticket(Vehicle v, ParkingSpace pSpace)
    {
        this.ticketId= UUID.randomUUID().toString();
        this.entryTime= LocalDateTime.now();
        this.isActive= true;
        this.vehicle= v;
        this.pSpaceAssigned= pSpace;
    }
}
