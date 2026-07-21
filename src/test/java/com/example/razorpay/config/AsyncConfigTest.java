package com.example.razorpay.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Unit tests for {@link AsyncConfig}.
 *
 * <p>
 * Verifies the asynchronous executor configuration used for webhook processing.
 * </p>
 *
 * <ul>
 * <li>Executor bean creation</li>
 * <li>Executor type</li>
 * <li>Thread pool configuration</li>
 * <li>Thread name prefix</li>
 * </ul>
 *
 * @author Zain
 * @since 1.0
 */
class AsyncConfigTest {

	/**
	 * Verifies that the webhook executor bean is created successfully.
	 */
	@Test
	void webhookExecutor_shouldCreateExecutorBean() {

		AsyncConfig config = new AsyncConfig();

		Executor executor = config.webhookExecutor();

		assertNotNull(executor);
		assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
	}

	/**
	 * Verifies that the executor contains the expected thread pool settings.
	 */
	@Test
	void webhookExecutor_shouldConfigureThreadPoolCorrectly() {

		AsyncConfig config = new AsyncConfig();

		ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.webhookExecutor();

		assertEquals(5, executor.getCorePoolSize());
		assertEquals(10, executor.getMaxPoolSize());
		assertEquals(100, executor.getQueueCapacity());
		assertEquals("Webhook-", executor.getThreadNamePrefix());
	}
}