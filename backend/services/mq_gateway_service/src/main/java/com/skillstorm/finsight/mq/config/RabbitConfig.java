package com.skillstorm.finsight.mq.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

  @Bean
  public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory,
      Jackson2JsonMessageConverter converter
  ) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(converter);
    return template;
  }

  @Bean
  public TopicExchange eventsExchange() {
    return ExchangeBuilder.topicExchange(MessagingTopology.EXCHANGE_EVENTS)
        .durable(true)
        .build();
  }

  @Bean
  public DirectExchange eventsDlx() {
    return ExchangeBuilder.directExchange(MessagingTopology.EXCHANGE_EVENTS_DLX)
        .durable(true)
        .build();
  }

  @Bean
  public Queue eventsQueue() {
    return QueueBuilder.durable(MessagingTopology.QUEUE_EVENTS)
        .withArguments(Map.of(
            "x-dead-letter-exchange", MessagingTopology.EXCHANGE_EVENTS_DLX,
            "x-dead-letter-routing-key", MessagingTopology.DLQ_ROUTING_KEY
        ))
        .build();
  }

  @Bean
  public Queue eventsDlq() {
    return QueueBuilder.durable(MessagingTopology.QUEUE_EVENTS_DLQ).build();
  }

  @Bean
  public Binding eventsBinding(
      Queue eventsQueue,
      TopicExchange eventsExchange
  ) {
    return BindingBuilder.bind(eventsQueue)
        .to(eventsExchange)
        .with(MessagingTopology.BINDING_EVENTS);
  }

  @Bean
  public Binding dlqBinding(
      Queue eventsDlq,
      DirectExchange eventsDlx
  ) {
    return BindingBuilder.bind(eventsDlq)
        .to(eventsDlx)
        .with(MessagingTopology.DLQ_ROUTING_KEY);
  }
}
