package com.HLDLLD.structural.Decorator.decorator;

import com.HLDLLD.structural.Decorator.Pizza;


public abstract class PizzaDecorator extends Pizza {
    protected Pizza decoratedPizza;

    public PizzaDecorator(Pizza pizza) {
        this.decoratedPizza = pizza;
    }

    @Override
    public String getDescription() {
        return decoratedPizza.getDescription();
    }

    @Override
    public double getCost() {
        return decoratedPizza.getCost();
    }
}
