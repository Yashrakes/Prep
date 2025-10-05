package com.HLDLLD.creational;

public class Singleton {

    private static volatile Singleton instance ;
    private String data;
    private Singleton(String data){
        this.data = data;
    }
    // we can return staic obbject from a non staic method , vice versa is not true
    /* 1.  using if condition so that this static instance is created onhly once
    if once created then it wont be null anymore

    2. now ques arrises if two threads are been using at same time , then it that case
    the instance is null for both threads , and we end up in declaring two instance

    so solution for this is use synchronized block so that a thread uses thois peice of code one at a time

    3. now the prob arises each time thread need to wait for the block now , whoch may slows down the process for the
    instance to get , so this can be solved by double checki locking idiom and by limiting synchronization

    4. we use volatile keyword for the instance object , because it prervents the crash that can be caused in
    multithreading
    as volatile will read from memory directly , ensures complete intialization of object.
    consider two thread
    thread a is first to access it and begins to intialize a value
    so the partially initialized var can refernce a prtially constructed object
    so thread b will consider it as not null , and threrad b willl not wait for synchronized and execute the
    code with partial initialized var instance,
    */

    public static Singleton getInstance(String data){
        Singleton result = instance; // by declaring this , we reduce the memory usage by 40 percent as volatile keyword each time need to read from memory6 , so we store it in a variable
        if(result==null) {
            synchronized (Singleton.class) {
                result = instance;
                if (result == null) {
                    instance = result =  new Singleton(data);
                }
            }
        }
        return  instance;
    }
}


//Why volatile?
//volatile ensures thread safety and prevents instruction reordering:
//        Problem 1: Instruction Reordering
//        Object creation involves 3 steps:
//
//        Allocate memory
//        Initialize the object
//        Assign memory address to variable
//
//        The compiler might reorder these steps for optimization:
//
//        Allocate memory
//        Assign address to variable (before initialization!)
//        Initialize the object
//
//        This means another thread could see a non-null reference to an uninitialized object!
//        Problem 2: Memory Visibility
//        Without volatile, changes made by one thread might not be immediately visible to other threads due to CPU caching.
//        What volatile Guarantees:
//
//        Prevents reordering: Ensures the assignment happens AFTER complete object construction
//        Memory visibility: When one thread writes to volatile variable, other threads immediately see it
//        Memory barrier: Creates synchronization points that prevent problematic optimizations
//
//        The Complete Picture: