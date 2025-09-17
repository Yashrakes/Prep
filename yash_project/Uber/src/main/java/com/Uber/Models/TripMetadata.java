package com.Uber.Models;

import com.Uber.Enums.RatingStatus;

public class TripMetadata {
    Location src;
    Location dest;
    RatingStatus riderRating;
    RatingStatus driverRating;

    public TripMetadata(Location src, Location dest, RatingStatus riderRating) {
        this.src = src;
        this.dest = dest;
        this.riderRating = riderRating;
        this.driverRating = null;
    }

    public Location getSrc() {
        return src;
    }

    public void setSrc(Location src) {
        this.src = src;
    }

    public Location getDest() {
        return dest;
    }

    public void setDest(Location dest) {
        this.dest = dest;
    }

    public RatingStatus getRiderRating() {
        return riderRating;
    }

    public void setRiderRating(RatingStatus riderRating) {
        this.riderRating = riderRating;
    }

    public RatingStatus getDriverRating() {
        return driverRating;
    }

    public void setDriverRating(RatingStatus driverRating) {
        this.driverRating = driverRating;
    }
}
