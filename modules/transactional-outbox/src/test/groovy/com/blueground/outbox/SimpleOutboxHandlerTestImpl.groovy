package com.blueground.outbox

import com.blueground.outbox.item.OutboxType
import com.fasterxml.jackson.databind.ObjectMapper

import java.time.Clock
import java.util.function.Function

class SimpleOutboxHandlerTestImpl extends SimpleOutboxHandler<OutboxPayloadTest> {

  private Function<OutboxPayloadTest, Void> handleWithParsedPayloadCallback

  SimpleOutboxHandlerTestImpl(
    OutboxType supportedType,
    ObjectMapper objectMapper,
    Clock clock,
    int maxRetries,
    Class<OutboxPayloadTest> payloadType,
    Function<OutboxPayloadTest, Void> handleWithParsedPayloadCallback
  ) {
    super(supportedType, objectMapper, clock, maxRetries, payloadType)
    this.handleWithParsedPayloadCallback = handleWithParsedPayloadCallback
  }

  @Override
  void handleFailure(String payload) {

  }

  @Override
  void handleWithParsedPayload(OutboxPayloadTest payload) {
    this.handleWithParsedPayloadCallback.apply(payload)
  }
}
