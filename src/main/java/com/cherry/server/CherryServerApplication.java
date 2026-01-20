package com.cherry.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CherryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CherryServerApplication.class, args);
	}

}
