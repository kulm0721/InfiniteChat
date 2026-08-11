package com.shanyangcode.offlinedataservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="AiService")
public interface AiServiceClient {
    @GetMapping("/api/ai/summary")
    String chatSummary(@RequestParam("historyLog") String historyLog);
}
