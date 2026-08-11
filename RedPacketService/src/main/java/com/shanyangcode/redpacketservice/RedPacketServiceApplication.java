package com.shanyangcode.redpacketservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.shanyangcode.redpacketservice", "com.shanyangcode.common"})
public class RedPacketServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedPacketServiceApplication.class, args);
	}

}
