package com.HLDLLD.structural.Facade;

public class DVDPlayer {
    public void on() {
        System.out.println("DVD Player on");
    }

    public void play(String movie) {
        System.out.println("Playing " + movie);
    }
}
