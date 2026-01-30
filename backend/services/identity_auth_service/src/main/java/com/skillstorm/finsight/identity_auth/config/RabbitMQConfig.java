package com.skillstorm.finsight.identity_auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Value("${finsight.rabbitmq.queues.auth-events:finsight.auth.events}")
    private String authEventsQueue;

    @Bean
    public Queue authEventsQueue() {
        return new Queue(authEventsQueue, true);
    }

    /**
     * RabbitAdmin declares queues to RabbitMQ when the connection is established.
     * Ensures finsight.ctr.events and finsight.sar.events exist in the broker.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    /**
     * Runs after startup to confirm RabbitMQ connection and queue declaration.
     */
    @Bean
    public ApplicationRunner rabbitMQStartupRunner(RabbitAdmin rabbitAdmin) {
        return args -> {
            rabbitAdmin.initialize();
            log.info("RabbitMQ connected. Queues declared: {}", authEventsQueue);
        };
    }
}
