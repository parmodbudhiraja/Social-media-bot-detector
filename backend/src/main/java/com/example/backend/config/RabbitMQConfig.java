package com.example.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "instagram.analysis.exchange";
    public static final String INFERENCE_QUEUE = "ml.inference.queue";
    public static final String RESULTS_QUEUE = "ml.results.queue";
    public static final String ROUTING_KEY_INFERENCE = "ml.inference";
    public static final String ROUTING_KEY_RESULTS = "ml.results";

    @Bean
    public Queue inferenceQueue() {
        return new Queue(INFERENCE_QUEUE, true);
    }

    @Bean
    public Queue resultsQueue() {
        return new Queue(RESULTS_QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding inferenceBinding(Queue inferenceQueue, TopicExchange exchange) {
        return BindingBuilder.bind(inferenceQueue).to(exchange).with(ROUTING_KEY_INFERENCE);
    }

    @Bean
    public Binding resultsBinding(Queue resultsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(resultsQueue).to(exchange).with(ROUTING_KEY_RESULTS);
    }
}
