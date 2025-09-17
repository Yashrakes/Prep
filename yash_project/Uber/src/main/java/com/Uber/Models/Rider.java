package com.Uber.Models;

import com.Uber.Enums.RatingStatus;

public class Rider {
    private String name;
    private RatingStatus rating;

    public Rider(String name, RatingStatus rating) {
        this.name = name;
        this.rating = rating;
    }

    public RatingStatus getRating() {
        return rating;
    }

    public void setRating(RatingStatus rating) {
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
