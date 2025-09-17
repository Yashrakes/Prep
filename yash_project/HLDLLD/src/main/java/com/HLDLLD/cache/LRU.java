package com.HLDLLD.cache;

import java.util.HashMap;
// head ---list ---- prev  next is on -> and <-  prev is on
//left --- list ---- right
class Node<K,V>{
    K key;
    V val;
    Node<K, V> prev;
    Node<K, V> next;
    Node(K key, V val){
        this.key = key;
        this.val = val;
        this.next=null;
        this.prev=null;
    }
}
public class LRU<K,V> implements Cache<K,V>{
    int capacity;
    public Node<K,V> left;
    public Node<K,V> right;
    HashMap<K, Node<K, V>> pres;

    public LRU(int c){
        this.capacity = c;
        left = new Node(null,null);
        right = new Node(null,null);
        pres = new HashMap();
        left.next = right;
        right.prev = left;
    }

    @Override
    public V get(K Key) {
       if(pres.containsKey(Key)){
           Node<K,V> node = pres.get(Key);
           put(Key, node.val);
           return node.val;
       }
        System.out.println("Key - "+ Key +" is not present in cache.");
        return null;
    };

    @Override
    public void put(K Key, V Value) {
        if(pres.containsKey(Key)){
            remove(pres.get(Key));
        }
        if(size() ==capacity){
            remove(right.prev);
        }
        insert(Key,Value);

    }

    @Override
    public int size() {
        return pres.size();
    }

    public void remove(Node<K,V> node){
        pres.remove(node.key);
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    public void insert(K key , V val){
        Node newN = new Node(key, val);
        pres.put(key, newN);
        newN.next = left.next;
        newN.next.prev = newN;
        left.next = newN;
        newN.prev = left;
    }

}
