package com.HLDLLD.creational;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

 abstract class noti{
    abstract String notifyU();
}

class email extends noti{
   public String notifyU(){
    return "send email";
   }
}

class sms extends noti{
    public String notifyU(){
        return "send sms";
    }
}
public class Factory {
    public static noti createn(String type){

        if(type.equals("email")){
            return new email();
        }
        if(type.equals("sms")){
            return new sms();
        }
        return null;
    }
}

 class main{
   // public static void main(String[] args) {
       public noti notif(String passed){
           //passed = "email";
            noti notification = Factory.createn(passed);
            System.out.println(notification.notifyU());
       // }
           return notification;
    }
}



//         Loose Coupling:
//        The client code doesn't need to know about the specific vehicle classes. It interacts with the Vehicle interface and the VehicleFactory.
//        Extensibility:
//        Adding a new vehicle type only requires creating a new concrete class and updating the VehicleFactory with a new case statement in the createVehicle method.
//        Maintainability:
//        The object creation logic is centralized, making it easier to manage and update. Changes to the object creation process don't require modifications in the client code.
//        Abstraction:
//        The factory provides a layer of abstraction between the client and the concrete implementations.
//


//// Interface
//interface Vehicle {
//    String getType();
//}
//
//// Concrete classes
//class Car implements Vehicle {
//    @Override
//    public String getType() {
//        return "Car";
//    }
//}
//
//class Motorcycle implements Vehicle {
//    @Override
//    public String getType() {
//        return "Motorcycle";
//    }
//}
//
//class Truck implements Vehicle {
//    @Override
//    public String getType() {
//        return "Truck";
//    }
//}
//
//class VehicleFactory {
//    public static Vehicle createVehicle(String type) {
//        switch (type.toLowerCase()) {
//            case "car":
//                return new Car();
//            case "motorcycle":
//                return new Motorcycle();
//            case "truck":
//                return new Truck();
//            default:
//                throw new IllegalArgumentException("Invalid vehicle type: " + type);
//        }
//    }
//}

//public class Main {
//    public static void main(String[] args) {
//        Vehicle car = VehicleFactory.createVehicle("car");
//        System.out.println("Created a: " + car.getType());
//
//        Vehicle motorcycle = VehicleFactory.createVehicle("motorcycle");
//        System.out.println("Created a: " + motorcycle.getType());
//
//        Vehicle truck = VehicleFactory.createVehicle("truck");
//        System.out.println("Created a: " + truck.getType());
//    }
//}
