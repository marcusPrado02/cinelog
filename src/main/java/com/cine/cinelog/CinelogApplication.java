package com.cine.cinelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cine.cinelog")
public class CinelogApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinelogApplication.class, args);
	}

}
