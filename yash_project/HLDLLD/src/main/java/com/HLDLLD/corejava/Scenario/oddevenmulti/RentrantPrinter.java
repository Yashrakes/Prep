package com.HLDLLD.corejava.Scenario.oddevenmulti;

import java.util.concurrent.locks.*;

class RentrantPrinter {
    private int number = 1;
    private final int MAX = 20;

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public void printOdd() {
        lock.lock();
        try {
            while (number <= MAX) {
                if (number % 2 == 0) {
                    condition.await();
                } else {
                    System.out.println("Odd: " + number++);
                    condition.signal();
                }
            }
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }

    public void printEven() {
        lock.lock();
        try {
            while (number <= MAX) {
                if (number % 2 != 0) {
                    condition.await();
                } else {
                    System.out.println("Even: " + number++);
                    condition.signal();
                }
            }
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }
}
// 🔥 2. What is Condition?
//
//👉 Replacement for:
//
//wait()
//notify()
//
//👉 But MUCH more powerful
//condition.await();   // like wait()
//condition.signal();  // like notify()
//condition.signalAll();

//Thread calls await():
//    → releases lock
//    → goes to waiting queue
//    → sleeps
//
//signal():
//    → wakes one thread
//    → thread tries to re-acquire lock
//await() must be called inside lock

//
//🔥 4. Advanced Features (BIG ADVANTAGE)
//        Feature	wait/notify	Condition
//        Multiple queues	❌	✅
//        Timeout	⚠️ limited	✅
//        Interrupt handling	⚠️ basic	✅
//        Fairness support	❌	✅
//        Try lock	❌	✅

//
//❓ Should Condition always be used with ReentrantLock?
//
//        👉 YES — ALWAYS
//
//        🧠 Why?
//
//        👉 Condition is created from Lock:
//
//        Lock lock = new ReentrantLock();
//        Condition condition = lock.newCondition();
//
//        👉 It is tightly coupled to the lock
//
//        ❌ You CANNOT use Condition with:
//synchronized
//normal objects
//        ⚠️ Important Rule
//
//        👉 await() and signal() ONLY work when:
//
//        lock.lock();
//        🔥 Comparison Summary
//        Concept	wait/notify	Condition
//        Works with	synchronized	Lock (ReentrantLock)
//        Multiple queues	❌	✅
//        Control	❌ random	✅ precise
//        Readability	⚠️	✅
//        Advanced features	❌	✅
//        🧠 When to Use What?
//        ✅ Use wait/notify when:
//        Simple problem
//        Interview basics
//        ✅ Use Condition when:
//        Multiple conditions needed
//        Complex coordination
//        Production code
//        💡 Final Intuition
//
//        👉 wait/notify =
//
//        “One waiting room, random wake-ups”
//
//        👉 Condition =
//
//        “Multiple queues, controlled signaling”