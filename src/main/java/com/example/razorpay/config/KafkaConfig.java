package com.example.razorpay.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

import com.example.razorpay.kafka.TopicNames;

@Configuration
@EnableKafka
public class KafkaConfig {
	@Bean
	NewTopic paymentTopic() {

		return TopicBuilder.name(TopicNames.PAYMENT_EVENTS).partitions(3).replicas(1).build();
	}

}
