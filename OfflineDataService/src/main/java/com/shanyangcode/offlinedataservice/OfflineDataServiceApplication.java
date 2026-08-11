package com.shanyangcode.offlinedataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.shanyangcode.offlinedataservice", "com.shanyangcode.common"})
@EnableFeignClients(basePackages = "com.shanyangcode.offlinedataservice.client")
public class OfflineDataServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OfflineDataServiceApplication.class, args);
	}

}
