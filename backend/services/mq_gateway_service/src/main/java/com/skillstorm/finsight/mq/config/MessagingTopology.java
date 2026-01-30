package com.skillstorm.finsight.mq.config;

public final class MessagingTopology {
  private MessagingTopology() {}

  public static final String EXCHANGE_EVENTS = "finsight.events.topic";

  public static final String QUEUE_EVENTS = "finsight.events.q";
  public static final String QUEUE_EVENTS_DLQ = "finsight.events.dlq";

  public static final String RK_EVENTS = "events.#";
  public static final String RK_EVENTS_DLQ = "events.dlq";
}
