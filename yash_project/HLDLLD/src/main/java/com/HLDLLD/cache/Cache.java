package com.HLDLLD.cache;

public interface Cache <K,V>{
     V get(K Key);
     void put(K Key , V Value);
     int size();
}
