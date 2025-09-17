package com.carRental.carRental;

import com.carRental.carRental.Product.Vehicle;
import com.carRental.carRental.Product.VehicleType;

import java.util.ArrayList;
import java.util.List;

public class Store {
    int storeId;
    VehicleInventoryManagement vehicleInventoryManagement ;
    Location storeLocation;
    List<Reservation> reservations;

    public Store(int id,List<Vehicle> vehicles){
        this.storeId = id;
        this.reservations = new ArrayList<>();
        this.vehicleInventoryManagement = new VehicleInventoryManagement(vehicles);
    }

    public List<Vehicle> getVehicles(VehicleType vehicleType) {

        return vehicleInventoryManagement.getVehicles();
    }

    public Reservation createReservation(Vehicle vehicle, User user){
        Reservation reservation = new Reservation();
        reservation.createReserve(user,vehicle);
        reservations.add(reservation);
        return reservation;
    }

    public boolean completeReservation(int reservationID) {

        //take out the reservation from the list and call complete the reservation method.
        return true;
    }

    //update reservation


}
