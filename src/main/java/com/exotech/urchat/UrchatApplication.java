package com.exotech.urchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class UrchatApplication {

	public static void main(String[] args) {
		System.setProperty("server.address", "0.0.0.0");
		SpringApplication.run(UrchatApplication.class, args);
	}

}
