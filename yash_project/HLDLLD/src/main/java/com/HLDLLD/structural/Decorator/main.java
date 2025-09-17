package com.HLDLLD.structural.Decorator;


import com.HLDLLD.structural.Decorator.decorator.CheeseDecorator;
import com.HLDLLD.structural.Decorator.decorator.OliveDecorator;

public class main {
    public static void main(String[] args) {
        Pizza basicPizza = new MargheritaPizza();
        System.out.println(basicPizza.getDescription() + " Cost: " + basicPizza.getCost());

        // Adding cheese topping
       // Pizza cheese = new OliveDecorator(new CheeseDecorator(new MargheritaPizza()));
        Pizza cheesePizza = new CheeseDecorator(basicPizza);
        System.out.println(cheesePizza.getDescription() + " Cost: " + cheesePizza.getCost());

        // Adding olives topping
        Pizza oliveCheesePizza = new OliveDecorator(cheesePizza);
        System.out.println(oliveCheesePizza.getDescription() + " Cost: " + oliveCheesePizza.getCost());

        Pizza olive = new OliveDecorator(basicPizza);
        System.out.println(olive.getDescription() + " Cost : " + olive.getCost());
    }
}
