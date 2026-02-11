package com.skillstorm.finsight.documents_cases.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    /**
     * Use JSON for message bodies so DocumentUploadEvent (and other POJOs) are serialized
     * instead of requiring Serializable (SimpleMessageConverter).
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Value("${finsight.rabbitmq.queues.ctr-events:finsight.ctr.events}")
    private String ctrEventsQueue;

    @Value("${finsight.rabbitmq.queues.sar-events:finsight.sar.events}")
    private String sarEventsQueue;

    @Bean
    public Queue ctrEventsQueue() {
        return new Queue(ctrEventsQueue, true);
    }

    @Bean
    public Queue sarEventsQueue() {
        return new Queue(sarEventsQueue, true);
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
            log.info("RabbitMQ connected. Queues declared: {}, {}", ctrEventsQueue, sarEventsQueue);
        };
    }
}
