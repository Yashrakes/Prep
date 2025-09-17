package com.ParkingLot.ParkingLot.parking;

import java.time.temporal.ChronoUnit;

public class DayPaymentStrategy extends PaymentStrategy{

    public DayPaymentStrategy(){
        this.setBikeCharges(100);
        this.setCarCharges(200);
        this.setTruckCharges(300);
    }

    @Override
    public double calculateCost(Ticket ticket) {

        long days = ChronoUnit.DAYS.between(ticket.getExitTime(), ticket.getEntryTime());
        return days * getChargeType(ticket.getVehicle());
    }
}
