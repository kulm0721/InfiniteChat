package com.shanyangcode.realtimeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "UserService")
public interface UserServiceClient {
    @GetMapping("/api/user/get/receivers")
    List<Long> getUserIdBySessionId(@RequestParam("sessionId") Long sessionId);
}
