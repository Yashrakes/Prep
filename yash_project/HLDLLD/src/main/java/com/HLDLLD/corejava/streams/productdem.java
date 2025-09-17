package com.HLDLLD.corejava.streams;

import lombok.Data;


public class productdem {
    private String name;
    private int price;

    @Override
    public String toString() {
        return "productdem{" +
                "name='" + name + '\'' +
                ", price=" + price +
                '}';
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public productdem(String name, int price){
        this.name = name;
        this.price = price;
    }

}
