package com.shanyangcode.realtimeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.shanyangcode.realtimeservice", "com.shanyangcode.common"})
@EnableFeignClients(basePackages = "com.shanyangcode.realtimeservice.client")
public class RealTimeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealTimeServiceApplication.class, args);
    }

}
