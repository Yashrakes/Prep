package com.Uber.Models;

import com.Uber.Enums.RatingStatus;


public class Driver {
    private String name;
    private RatingStatus ratingStatus;

    public Driver(String name, RatingStatus ratingStatus) {
        this.name = name;
        this.ratingStatus = ratingStatus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RatingStatus getRatingStatus() {
        return ratingStatus;
    }

    public void setRatingStatus(RatingStatus ratingStatus) {
        this.ratingStatus = ratingStatus;
    }
}
