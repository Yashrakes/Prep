package com.HLDLLD.corejava;

import java.util.*;

public class Mapp {
    public static void main(String args[])
    {
        // Creating an empty HashMap

        Map<String, Integer> hm
                = new HashMap<>();
// linked hasmap extend -> hasmap immplements class-> map interfac
        // treempa class inmplement -> orted map interface implement map- interface
        // Inserting pairs in above Map
        // using put() method
        hm.put("a", 100);
        hm.put("b",200);
        hm.put("c", 300);
        hm.put("d", 400);
        hm.remove("d");
        // Traversing through Map using for-each loop
        for (Map.Entry<String, Integer> me :
                hm.entrySet()) {

            // Printing keys
            System.out.print(me.getKey() + ":");
            System.out.println(me.getValue());
        }


        Map<String, String> map = new HashMap<String, String>();
        map.put("india" , "state");
        map.put(null, null);

        System.out.println(map.get("india"));
        map.remove("india");
        map.size();
        Set<String> keys = map.keySet();
        Iterator<String> itr = keys.iterator();
        while(itr.hasNext()){
            String key = itr.next();
        }
        for(String k : keys){
            System.out.println(map.get(k));
        }

        Set<Map.Entry<String, String>> entries = map.entrySet();
        Iterator<Map.Entry<String, String>> it = entries.iterator();

        while(it.hasNext()){
            Map.Entry<String, String> entry = it.next();
            String key = entry.getKey();
        }
        for(Map.Entry<String, String> entry:entries){
        }



    }
}
