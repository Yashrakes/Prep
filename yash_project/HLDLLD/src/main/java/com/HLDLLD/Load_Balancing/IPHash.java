package com.HLDLLD.Load_Balancing;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IPHash {
    private List<String> servers;

    public IPHash(List<String> servers) {
        this.servers = servers;
    }

    public String getNextServer(String clientIp) {
        int hash = clientIp.hashCode();
        int serverIndex = Math.abs(hash) % servers.size();
        return servers.get(serverIndex);
    }

    public static void main(String[] args) {
       // List<String> servers = Arrays.asList("Server1", "Server2", "Server3");
        List<String> servers = List.of("Server1", "Server2", "Server3");
        IPHash ipHash = new IPHash(servers);

        List<String> clientIps = List.of("192.168.0.1", "192.168.0.2", "192.168.0.3");
//        clientIps.stream().forEach((ip) -> {
//            System.out.println(ip + " is mapped to " + ipHash.getNextServer(ip));
//        });
//        clientIps.stream()
//                .map(ip -> ip + " is mapped to " + ipHash.getNextServer(ip))
//                .forEach(System.out::println);
        for (String ip : clientIps) {
            System.out.println(ip + " is mapped to " + ipHash.getNextServer(ip));
        }
    }
}
