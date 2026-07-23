package com.example.razorpay.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class for asynchronous task execution.
 *
 * <p>
 * Defines a dedicated thread pool used for processing webhook events
 * asynchronously. Using a separate executor prevents long-running webhook
 * operations from blocking the main request processing thread.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	private static final int CORE_POOL_SIZE = 5;
	private static final int MAX_POOL_SIZE = 10;
	private static final int QUEUE_CAPACITY = 100;
	private static final String THREAD_NAME_PREFIX = "Webhook-";

	/**
	 * Creates a thread pool executor for asynchronous webhook processing.
	 *
	 * @return configured {@link Executor} instance
	 */
	@Bean(name = "webhookExecutor")
	Executor webhookExecutor() {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(CORE_POOL_SIZE);
		executor.setMaxPoolSize(MAX_POOL_SIZE);
		executor.setQueueCapacity(QUEUE_CAPACITY);
		executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
		executor.initialize();

		return executor;
	}
}