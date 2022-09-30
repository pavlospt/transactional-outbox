package com.blueground.outbox

import com.blueground.outbox.item.OutboxItem
import com.blueground.outbox.item.OutboxStatus
import com.blueground.outbox.item.OutboxType
import com.blueground.outbox.store.OutboxStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OutboxItemProcessor(
  private val item: OutboxItem,
  private val handler: OutboxHandler,
  private val store: OutboxStore
) : Runnable {

  companion object {
    private const val LOGGER_PREFIX = "[OUTBOX-ITEM-PROCESSOR]"
    private val logger: Logger = LoggerFactory.getLogger(OutboxItemProcessor::class.java)
  }

  override fun run() {
    if (!handler.supports(item.type)) {
      logger.error("$LOGGER_PREFIX Handler ${handler::class.java} does not support item of type: ${item.type}")
      return
    }

    try {
      logger.info("$LOGGER_PREFIX Handling item with id: ${item.id} and type: ${item.type}")
      handler.handle(item.payload)
      item.status = OutboxStatus.COMPLETED
    } catch (exception: Exception) {
      if (handler.hasReachedMaxRetries(item.retries)) {
        handleTerminalFailure(exception)
      } else {
        handleRetryableFailure(exception)
      }
    } finally {
      store.update(item)
    }
  }

  private fun handleTerminalFailure(exception: Exception) {
    logger.info(
      "$LOGGER_PREFIX Failure handling outbox item with id: ${item.id} and type: ${item.type}. " +
        "Item reached max-retries (${item.retries}), delegating failure to handler.",
      exception
    )
    item.status = OutboxStatus.FAILED
    handler.handleFailure(item.payload)
  }

  private fun handleRetryableFailure(exception: Exception) {
    item.retries += 1
    item.nextRun = handler.getNextExecutionTime(item.retries)
    item.status = OutboxStatus.PENDING
    logger.info(
      "$LOGGER_PREFIX Failure handling outbox item with id: ${item.id} and type: ${item.type}. " +
        "Updated retries (${item.retries}) and next run is on ${item.nextRun}.",
      exception
    )
  }

  private fun OutboxHandler.supports(type: OutboxType) = this.getSupportedType() == type
}
