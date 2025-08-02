package com.bankingsystem.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CoreBankingSystemApplication {

	private static final Logger logger = LoggerFactory.getLogger(CoreBankingSystemApplication.class);

	public static void main(String[] args) {
		logger.info("Starting Core Banking System Application...");
		SpringApplication.run(CoreBankingSystemApplication.class, args);
		logger.info("Application started successfully.");
	}

}
