package com.HLDLLD.corejava.Scenario;

import java.util.*;


// ==============================
// 🚫 1. WRONG IMPLEMENTATION
// ==============================
 class PersonWrong {
    private final List<String> hobbies;

    public PersonWrong(List<String> hobbies) {
        this.hobbies = hobbies; // ❌ direct reference (BAD)
    }

    public List<String> getHobbies() {
        return hobbies; // ❌ exposes internal state
    }
}

// ==============================
// ⚠️ 2. PARTIAL FIX (Constructor copy only)
// ==============================
class PersonPartialFix {
    private final List<String> hobbies;

    public PersonPartialFix(List<String> hobbies) {
        this.hobbies = new ArrayList<>(hobbies); // ✅ copy in constructor
    }

    public List<String> getHobbies() {
        return hobbies; // ❌ still exposes internal state
    }
}

// ==============================
// ⚠️ 3. PARTIAL FIX (Getter copy only)
// ==============================
class PersonPartialFix2 {
    private final List<String> hobbies;

    public PersonPartialFix2(List<String> hobbies) {
        this.hobbies = hobbies; // ❌ still wrong
    }

    public List<String> getHobbies() {
        return new ArrayList<>(hobbies); // ✅ safe getter
    }
}

// ==============================
// ✅ 4. FULLY IMMUTABLE (BEST)
// ==============================
final class PersonImmutable {
     // record also don't fis this issue defensive copy you have to maually weritr constructor in that as well

    private final List<String> hobbies;

    public PersonImmutable(List<String> hobbies) {
        // ✅ defensive copy + immutable
        this.hobbies = List.copyOf(hobbies);
        //List<String> list1 = List.of(original.toArray(new String[0]));
//        List<String> list = List.of("A", "B");
//        List<String> copy = List.copyOf(list);
    }

    public List<String> getHobbies() {
        return hobbies; // ✅ safe (already immutable)
    }
}

// ==============================
// 🔥 MAIN DEMO CLASS
// ==============================
public class ImmutabilityDemo {

    public static void main(String[] args) {

        System.out.println("===== WRONG IMPLEMENTATION =====");
        List<String> list1 = new ArrayList<>();
        list1.add("Cricket");

        PersonWrong p1 = new PersonWrong(list1);
        list1.add("Hacking"); // 😈 external modification

        System.out.println(p1.getHobbies());
        // ❌ [Cricket, Hacking] → BROKEN


        System.out.println("\n===== PARTIAL FIX (Constructor only) =====");
        List<String> list2 = new ArrayList<>();
        list2.add("Reading");

        PersonPartialFix p2 = new PersonPartialFix(list2);
        p2.getHobbies().add("Gaming"); // 😈 modifies internal state

        System.out.println(p2.getHobbies());
        // ❌ [Reading, Gaming] → STILL BROKEN


        System.out.println("\n===== PARTIAL FIX (Getter only) =====");
        List<String> list3 = new ArrayList<>();
        list3.add("Music");

        PersonPartialFix2 p3 = new PersonPartialFix2(list3);
        list3.add("Dance"); // 😈 still affects object

        System.out.println(p3.getHobbies());
        // ❌ [Music, Dance] → STILL BROKEN


        System.out.println("\n===== FULLY IMMUTABLE =====");
        List<String> list4 = new ArrayList<>();
        list4.add("Travel");

        PersonImmutable p4 = new PersonImmutable(list4);

        list4.add("Hack"); // 😈 will NOT affect object

        System.out.println(p4.getHobbies());
        // ✅ [Travel]

        // Try modifying returned list
        try {
            p4.getHobbies().add("Break");
        } catch (Exception e) {
            System.out.println("Modification not allowed: " + e);
        }
    }
}



//👉 Q: Does final class mean immutable?
//
//        ❌ Answer: NO
//
//        🔥 Example of Mutable Final Class
//final class Counter {
//    private int count;
//
//    public void increment() {
//        count++;
//    }
//}
//
//👉 Fully mutable despite being final
//
//✅ Immutable Class Requires
//final class
//private final fields



// record also don't fis this issue defensive copy you have to maually weritr constructor in that as well


//List<String> list1 = List.of(original.toArray(new String[0]));
//        List<String> list = List.of("A", "B");
//        List<String> copy = List.copyOf(list);
