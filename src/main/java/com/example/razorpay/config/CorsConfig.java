package com.example.razorpay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for Cross-Origin Resource Sharing (CORS).
 *
 * <p>
 * Enables cross-origin requests for all application endpoints. This
 * configuration is suitable for development environments. For production,
 * replace the wildcard origin with specific trusted domains.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Configuration
public class CorsConfig {

	/**
	 * Configures global CORS settings.
	 *
	 * @return {@link WebMvcConfigurer} containing CORS configuration
	 */
	@Bean
	WebMvcConfigurer corsConfigurer() {

		return new WebMvcConfigurer() {

			@Override
			public void addCorsMappings(CorsRegistry registry) {

				registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
			}
		};
	}
}