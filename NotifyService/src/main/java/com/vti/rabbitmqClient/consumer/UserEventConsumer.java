package com.vti.rabbitmqClient.consumer;

import com.vti.rabbitmqClient.constants.Constants;
import com.vti.rabbitmqClient.service.NotifyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    @Autowired
    private NotifyHandler notifyHandler;

    @RabbitHandler
    @RabbitListener(queues = Constants.QUEUE_USER_CREATED)
    public void receiveMessage(String message) {
        log.info("Received user.created event from AuthService: {}", message);
        notifyHandler.handleUserNotification(message);
    }
}