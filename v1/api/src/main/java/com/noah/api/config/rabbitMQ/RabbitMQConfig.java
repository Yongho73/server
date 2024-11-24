package com.noah.api.config.rabbitMQ;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "exchanges-smartix-tcm";
    public static final String QUEUE_NAME = "queue-smartix-tcm";

    // Fanout Exchange 설정
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    // Queue 설정
    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true); // durable = true
    }

    // Exchange와 Queue 바인딩
    @Bean
    public Binding binding(FanoutExchange exchange, Queue queue) {
        return BindingBuilder.bind(queue).to(exchange);
    }
}