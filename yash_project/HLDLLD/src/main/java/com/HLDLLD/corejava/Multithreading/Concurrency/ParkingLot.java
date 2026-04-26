package com.HLDLLD.corejava.Multithreading.Concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

enum VehicleType {
    MOTORCYCLE, CAR, TRUCK
}

class ParkingSpot {
    private final int id;
    private final VehicleType type;
    private boolean occupied;
    private final ReentrantLock lock = new ReentrantLock();

    public ParkingSpot(int id, VehicleType type) {
        this.id = id;
        this.type = type;
        this.occupied = false;
    }

    public boolean occupy() {
        lock.lock();
        try {
            if (occupied) return false;
            occupied = true;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void vacate() {
        lock.lock();
        try {
            occupied = false;
        } finally {
            lock.unlock();
        }
    }

    public VehicleType getType() {
        return type;
    }
}


public class ParkingLot {

    // Free spots per vehicle type
    private final ConcurrentHashMap<VehicleType, BlockingQueue<ParkingSpot>> freeSpots;

    // Ticket → ParkingSpot
    private final ConcurrentHashMap<String, ParkingSpot> activeTickets;

    // Available count per type
    private final ConcurrentHashMap<VehicleType, AtomicInteger> availableCount;

    private final AtomicInteger ticketCounter = new AtomicInteger(0);

    public ParkingLot() {
        freeSpots = new ConcurrentHashMap<>();
        activeTickets = new ConcurrentHashMap<>();
        availableCount = new ConcurrentHashMap<>();

        for (VehicleType type : VehicleType.values()) {
            freeSpots.put(type, new LinkedBlockingQueue<>());
            availableCount.put(type, new AtomicInteger(0));
        }
    }

    // Add spot (admin/setup operation)
    public void addParkingSpot(ParkingSpot spot) {
        freeSpots.get(spot.getType()).offer(spot);
        availableCount.get(spot.getType()).incrementAndGet();
    }

    // O(1)
    public String parkVehicle(VehicleType type) {
        ParkingSpot spot = freeSpots.get(type).poll();
        if (spot == null) {
            return null; // no spot available
        }

        if (!spot.occupy()) {
            return null; // extremely rare race case
        }

        String ticket = "TICKET-" + ticketCounter.incrementAndGet();
        activeTickets.put(ticket, spot);
        availableCount.get(type).decrementAndGet();
        return ticket;
    }

    // O(1)
    public boolean unparkVehicle(String ticket) {
        ParkingSpot spot = activeTickets.remove(ticket);
        if (spot == null) return false;

        spot.vacate();
        freeSpots.get(spot.getType()).offer(spot);
        availableCount.get(spot.getType()).incrementAndGet();
        return true;
    }

    public int getAvailableSpots(VehicleType type) {
        return availableCount.get(type).get();
    }
}

