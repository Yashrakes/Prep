package com.HLDLLD.structural.Adaptor;

public class Demo {
    public static void main(String[] args) {
        RoundHole hole = new RoundHole(5);
        RoundPeg roundPeg = new RoundPeg(5);

        if (hole.fits(roundPeg)) {
            System.out.println("Round peg r5 fits round hole r5.");
        }

        SquarePeg squarePeg = new SquarePeg(4);
        SquarePegAdapter squarePegAdapter = new SquarePegAdapter(squarePeg);
        //boolean result1 = hole.fits(new RoundPeg(squarePegAdapter.getRadius()));
        boolean result1 = hole.fits(squarePegAdapter);
        if (result1) {
            System.out.println("Square peg w2 fits round hole r5.");
        }
    }
}
