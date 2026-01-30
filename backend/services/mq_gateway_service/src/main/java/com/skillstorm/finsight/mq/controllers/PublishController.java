package com.skillstorm.finsight.mq.controllers;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.mq.config.MessagingTopology;
import com.skillstorm.finsight.mq.models.EventEnvelope;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/mq")
public class PublishController {

  private final RabbitTemplate rabbitTemplate;

  public PublishController(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @PostMapping("/publish")
  public ResponseEntity<PublishResponse> publish(
      @Valid @RequestBody EventEnvelope event,
      @RequestParam(defaultValue = "events.default") String routingKey
  ) {

    rabbitTemplate.convertAndSend(
        MessagingTopology.EXCHANGE_EVENTS,
        routingKey,
        event
    );

    return ResponseEntity.accepted().body(
        new PublishResponse("accepted", MessagingTopology.EXCHANGE_EVENTS, routingKey)
    );
  }

  public record PublishResponse(String status, String exchange, String routingKey) {}
}
