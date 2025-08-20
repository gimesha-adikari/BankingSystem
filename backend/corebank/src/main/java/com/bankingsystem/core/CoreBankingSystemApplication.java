package com.bankingsystem.core;

import com.bankingsystem.core.features.accounts.application.impl.AccountServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CoreBankingSystemApplication {

	private static final Logger logger = LoggerFactory.getLogger(CoreBankingSystemApplication.class);

    AccountServiceImpl accountService;

	public static void main(String[] args) {
		logger.info("Starting Core Banking System Application...");
        System.setProperty("server.address", "0.0.0.0");
        System.setProperty("server.port", "8080");
        SpringApplication.run(CoreBankingSystemApplication.class, args);
		logger.info("Application started successfully.");
	}

}
