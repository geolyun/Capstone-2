package com.capstone.Capstone_2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class Capstone2Application {

	public static void main(String[] args) {
		SpringApplication.run(Capstone2Application.class, args);
	}

}
