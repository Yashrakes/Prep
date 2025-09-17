package com.HLDLLD.corejava;
import com.sun.source.tree.Tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

public class LRUCacheGenric<K, V> {

    private Node<K, V> head = new Node<>(null, null);
    private Node<K, V> tail = new Node<>(null, null);
    private Map<K, Node<K, V>> map = new HashMap<>();
    private int capacity;

    public LRUCacheGenric(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        if (map.containsKey(key)) {
            Node<K, V> node = map.get(key);
            remove(node);
            insert(node);
            return node.value;
        } else {
            //return null;
            throw new KeyNotFoundException("Key '" + key + "' not found in cache");
        }
    }

    public void put(K key, V value) {
        if (map.containsKey(key)) {
            remove(map.get(key));
        }
        if (map.size() == capacity) {
            remove(tail.prev);
        }
        insert(new Node<>(key, value));
    }

    private void remove(Node<K, V> node) {
        map.remove(node.key);
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void insert(Node<K, V> node) {
        map.put(node.key, node);
        node.next = head.next;
        node.next.prev = node;
        head.next = node;
        node.prev = head;
    }

    public static class Node<K, V> {
        Node<K, V> prev, next;
        K key;
        V value;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    public static void main(String[] args) {
        LRUCacheGenric<String, String> cache = new LRUCacheGenric<>(2);
        cache.put("one", "apple");

        cache.put("two", "banana");
        System.out.println(cache.get("one")); // apple
        cache.put("three", "mango"); // "two" will be evicted
        try {
            System.out.println(cache.get("two"));  // throws KeyNotFoundException
        } catch (LRUCacheGenric.KeyNotFoundException e) {
            System.out.println(e.getMessage());  // prints "Key '3' not found in cache"
        }
    }
    public static class KeyNotFoundException extends RuntimeException {
        public KeyNotFoundException(String message) {
            super(message);
        }
    }
}


