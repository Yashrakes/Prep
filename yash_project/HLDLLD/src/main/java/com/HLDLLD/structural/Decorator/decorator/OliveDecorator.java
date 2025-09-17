package com.HLDLLD.structural.Decorator.decorator;

import com.HLDLLD.structural.Decorator.Pizza;


public class
OliveDecorator extends PizzaDecorator {
    public OliveDecorator(Pizza pizza) {
        super(pizza);
    }

    @Override
    public String getDescription() {
        return decoratedPizza.getDescription() + ", Olives";
    }

    @Override
    public double getCost() {
        return decoratedPizza.getCost() + 30.0;
    }
}
