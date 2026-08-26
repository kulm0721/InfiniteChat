package com.shanyangcode.redpacketservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.shanyangcode.redpacketservice", "com.shanyangcode.common"})
@EnableFeignClients(basePackages = "com.shanyangcode.redpacketservice.client")
public class RedPacketServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedPacketServiceApplication.class, args);
	}

}
