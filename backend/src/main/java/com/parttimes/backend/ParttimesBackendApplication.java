package com.parttimes.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class ParttimesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParttimesBackendApplication.class, args);
	}
}
