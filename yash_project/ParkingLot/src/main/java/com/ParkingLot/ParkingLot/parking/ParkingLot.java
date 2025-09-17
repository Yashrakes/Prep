package com.ParkingLot.ParkingLot.parking;

import com.ParkingLot.ParkingLot.admin.Address;
import com.ParkingLot.ParkingLot.vehicle.Vehicle;
import com.ParkingLot.ParkingLot.vehicle.VehicleType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
public class ParkingLot {
    private String pLotId;
    private List<Floor> floorList;
    private List<EntryGate> entries;
    private List<ExitGate> exits;
    private Address address;
    private ParkingStrategy pStrategy;

    public static ParkingLot INSTANCE = new ParkingLot();

    private ParkingLot(){
        pLotId= UUID.randomUUID().toString();
        floorList = new ArrayList<>();
        entries= new ArrayList<>();
        exits= new ArrayList<>();
    }

    public boolean isParkingSpaceAvailable(VehicleType vType){
        for(Floor l : floorList){
            Boolean temp = l.canParkVehicle(vType);
            if(temp){
                return true;
            }
        }
        return false;
    }

    public ParkingSpace findParkingSpace(Vehicle v)
    {
        for (Floor floor : floorList) {
            ParkingSpace temp = floor.getSpace(v);
            if(Objects.nonNull(temp)) {
                return temp;
            }
        }
        return null;
    }





}
