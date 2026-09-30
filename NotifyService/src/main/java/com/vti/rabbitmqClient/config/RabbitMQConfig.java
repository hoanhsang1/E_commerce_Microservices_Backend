package com.vti.rabbitmqClient.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vti.rabbitmqClient.constants.Constants;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(Constants.EXCHANGE);
    }

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(Constants.QUEUE_USER_CREATED, true);
    }

    @Bean
    public Binding bindingUserCreated(Queue userCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userCreatedQueue).to(exchange).with(Constants.ROUTING_KEY_USER);
    }
}
