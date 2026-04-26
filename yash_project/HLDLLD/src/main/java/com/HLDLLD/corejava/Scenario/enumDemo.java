package com.HLDLLD.corejava.Scenario;

public class enumDemo {

    enum LightColor {

        RED {
            @Override
            public String getAction(boolean pedestrianWaiting, boolean emergencyVehicle) {
                return "Stop";
            }
        },

        YELLOW {
            @Override
            public String getAction(boolean pedestrianWaiting, boolean emergencyVehicle) {
                return pedestrianWaiting
                        ? "Stop, pedestrian crossing"
                        : "Slow down";
            }
        },

        GREEN {
            @Override
            public String getAction(boolean pedestrianWaiting, boolean emergencyVehicle) {
                return emergencyVehicle
                        ? "Give way to emergency vehicle"
                        : "Go";
            }
        };

        public abstract String getAction(boolean pedestrianWaiting, boolean emergencyVehicle);
    }

    public static void main(String[] args) {

        LightColor light = LightColor.YELLOW;

        boolean pedestrianWaiting = true;
        boolean emergencyVehicle = false;

        String action = light.getAction(pedestrianWaiting, emergencyVehicle);

        System.out.println(action);
    }
}