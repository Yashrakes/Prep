package com.HLDLLD.corejava.Scenario.oddevenmulti;

public class SyncPrinter {
    public int number = 1;
    private int max = 20;

    public synchronized void printOdd() {
        while (number <= max) {
            if (number % 2 != 0) {
                System.out.println(number);
                number ++;
                notify();
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    public synchronized void printeven() {
        while (number <= max) {
            if (number % 2 == 0) {
                System.out.println(number);
                number++;
                notify();
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

}

class main {
    public static void main(String[] args) {
        SyncPrinter syncPrinter = new SyncPrinter();

        Thread t1 = new Thread(() -> {
            syncPrinter.printOdd();
        });

        Thread t2 = new Thread(() -> {
            syncPrinter.printeven();
        });

        t1.start();
        t2.start();
    }
}
