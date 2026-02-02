package com.skillstorm.finsight.mq.config;

public final class MessagingTopology {
  private MessagingTopology() {}

  // Main exchange
  public static final String EXCHANGE_EVENTS = "finsight.events.topic";

  // Dead-letter exchange
  public static final String EXCHANGE_EVENTS_DLX = "finsight.events.dlx";

  public static final String QUEUE_EVENTS = "finsight.events.q";
  public static final String QUEUE_EVENTS_DLQ = "finsight.events.dlq";

  // Topic binding pattern
  public static final String BINDING_EVENTS = "events.#";

  // Literal routing key for DLQ
  public static final String DLQ_ROUTING_KEY = "events.dlq";
}