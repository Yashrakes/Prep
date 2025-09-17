package com.HLDLLD.cache;

public class LRUDemo {
    public static void main(String[] args) {
        LRU cache = new LRU(4);

        System.out.println("Size of cache - "+cache.size());
        cache.put(12, "Amit");
        System.out.println("For key "+12+" the value in cache is - "+cache.get(12));
        System.out.println("Size of cache - "+cache.size());
        cache.put("car", "Amit");
        System.out.println("For key car the value in cache is - "+cache.get("car"));
        System.out.println("Size of cache - "+cache.size());
        cache.put(10.12, "double");
        System.out.println("For key "+10.12+" the value in cache is - "+cache.get(10.12));
        System.out.println("Size of cache - "+cache.size());
        cache.put(6, "Lalit");
        System.out.println("For key "+6+" the value in cache is - "+cache.get(6));
        System.out.println("Size of cache - "+cache.size());
        cache.put(5, "Sumit");
        System.out.println("Size of cache - "+cache.size());
        System.out.println("For key "+12+" the value in cache is - "+cache.get(12));
        System.out.println("For key "+5+" the value in cache is - "+cache.get(5));
    }
}
