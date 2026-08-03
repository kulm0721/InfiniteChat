package com.shanyangcode.userservice.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

import java.util.*;

@SuppressWarnings({"all"})
public class ConsistentHash {
    public HashMap<String, ServiceInstance> map = new HashMap<>();
    private TreeMap<Integer, String> Nodes = new TreeMap<>();
    private int VIRTUAL_NODES = 160;
    private List<ServiceInstance> instances = new ArrayList<>();

    public ConsistentHash(List<ServiceInstance> instances) {
        this.instances = instances;
        init();
    }

    public void init() {
        for (ServiceInstance instance : instances) {
            String url = instance.getUri().toString();
            Nodes.put(getHash(url), url);
            map.put(url, instance);
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                int hash = getHash(url + "#" + i);
                Nodes.put(hash, url);
            }
        }
    }

    public String getServer(String clientInfo) {
        int hash = getHash(clientInfo);
        SortedMap<Integer, String> subMap = Nodes.tailMap(hash);

        Integer nodeIndex = subMap.isEmpty() ? Nodes.firstKey() : subMap.firstKey();

        return Nodes.get(nodeIndex);
    }

    private int getHash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;
            hash += hash << 13;
            hash ^= hash >> 7;
            hash += hash << 3;
            hash ^= hash >> 17;
            hash += hash << 5;
            if (hash < 0) {
                hash = Math.abs(hash);
            }
        }
        return hash;
    }
}
