package com.shanyangcode.userservice.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

import java.util.HashMap;
import java.util.List;

public class UrlHashLoadBalancer {
    public ServiceInstance select(List<ServiceInstance> instances, String userId) {
        ConsistentHash consistentHash = new ConsistentHash(instances);
        HashMap<String,ServiceInstance> map=consistentHash.map;
        return map.get(consistentHash.getServer(userId));
    }
}
