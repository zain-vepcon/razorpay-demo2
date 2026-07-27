package com.example.razorpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Entry point for the Razorpay Demo Spring Boot application.
 *
 * <p>
 * Bootstraps the Spring application context and starts the embedded web server.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@SpringBootApplication
@EnableKafka
@EnableCaching
public class RazorpayDemoApplication {

	/**
	 * Starts the Spring Boot application.
	 *
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(RazorpayDemoApplication.class, args);
	}
}