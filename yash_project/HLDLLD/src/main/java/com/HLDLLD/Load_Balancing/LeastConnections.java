package com.HLDLLD.Load_Balancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LeastConnections {
    private Map<String, Integer> serverConnections;

    public LeastConnections(List<String> servers) {
        AtomicInteger i = new AtomicInteger(6);
        serverConnections = new ConcurrentHashMap<>();

        //serverConnections = servers.stream().collect(Collectors.toMap(server->server,server->0));

        for (String server : servers) {
            serverConnections.put(server, i.getAndIncrement());
        }
    }

    public String getNextServer() {
        return serverConnections.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void releaseConnection(String server) {
        serverConnections.computeIfPresent(server, (k, v) -> v > 0 ? v - 1 : 0);
        //serverConnections.computeIfAbsent(server , (k) -> 0);
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<String> servers = List.of("Server1", "Server2", "Server3");
        LeastConnections leastConnectionsLB = new LeastConnections(servers);

        for (int i = 0; i < 6; i++) {
            executor.submit(() -> {
                        String server = leastConnectionsLB.getNextServer();
                        System.out.println(server);
                        leastConnectionsLB.releaseConnection(server);
                    }
            );

        }
        executor.shutdown();


//        for (int i = 0; i < 6; i++) {
//            Thread thread = new Thread(() -> {
//                String server = leastConnectionsLB.getNextServer();
//                System.out.println(server);
//                leastConnectionsLB.releaseConnection(server);
//            });
//            thread.start();
//        }





//        ExecutorService customExecutor = Executors.newFixedThreadPool(6);
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//        for (int i = 0; i < 6; i++) {
//            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                String server = leastConnectionsLB.getNextServer();
//                System.out.println(server);
//                leastConnectionsLB.releaseConnection(server);
//            }, customExecutor);
//
//            futures.add(future);
//        }
//
//// Wait for all to finish
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//// Shutdown the executor
//        customExecutor.shutdown();






//        ForkJoinPool forkJoinPool = new ForkJoinPool(6);  // Custom ForkJoinPool with 6 threads
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//        for (int i = 0; i < 6; i++) {
//            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                String server = leastConnectionsLB.getNextServer();
//                System.out.println(server);
//                leastConnectionsLB.releaseConnection(server);
//            }, forkJoinPool);
//
//            futures.add(future);
//        }
//
//// Wait for all to finish
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//// Shutdown the ForkJoinPool
//        forkJoinPool.shutdown();
    }
}
