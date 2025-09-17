package com.HLDLLD.structural.Decorator;

public class FarmHousePizza extends Pizza {
    @Override
    public String getDescription() {
        return "FarmHouse Pizza";
    }

    @Override
    public double getCost() {
        return 250.0;
    }
}
