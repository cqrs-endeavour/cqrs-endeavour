/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import akka.actor._
import scala.concurrent.duration._

object EventEnvelopeBatcher {
  case class BatchedEventEnvelopes(envelopes: Seq[EventEnvelope]) {
    override def toString = s"BatchedEventEnvelopes[envelopes.size=${envelopes.size}]"
  }

  case object ProcessBatch

  def props(limit: Int, flushInterval: FiniteDuration, recipient: ActorRef): Props = {
    Props(classOf[EventEnvelopeBatcher], limit, flushInterval, recipient)
  }
}

/**
 * Actor that can be used as a man in the middle between [[CQRSViewEventBus]] and some [[DomainView]] if
 * concrete view can benefit from batching.
 * 
 * @param limit max number of messages, when limit is reached batch is flushed
 * @param flushInterval time between flushes if number of messages did not reach [[limit]] 
 * @param recipient recipient of the [[cqrs.query.EventEnvelopeBatcher.BatchedEventEnvelopes]] message
 */
class EventEnvelopeBatcher(limit: Int, flushInterval: FiniteDuration, recipient: ActorRef) extends Actor with ActorLogging {
  import EventEnvelopeBatcher._
  import context.dispatcher

  private val bufferedEvents = new Array[EventEnvelope](limit)
  private var currentNumberOfEvents = 0
  private var currentTick: Option[Cancellable] = None

  override def receive: Receive = {
    case envelope: EventEnvelope => {
      if (currentNumberOfEvents == 0 && currentTick.isEmpty) {
        log.info("Scheduling new tick")
        currentTick = Some(context.system.scheduler.scheduleOnce(flushInterval, self, ProcessBatch))
      }
      else if (currentTick.isDefined) {
        currentTick.get.cancel()
        currentTick = Some(context.system.scheduler.scheduleOnce(flushInterval, self, ProcessBatch))
      }

      bufferedEvents(currentNumberOfEvents) = envelope
      currentNumberOfEvents += 1

      if (currentNumberOfEvents == limit) {
        sendBatchForProcessing()
      }
    }
    case query: DomainQuery => {
      // flushing current batch - we do not want to process DomainQuery when we have some events queued up
      sendBatchForProcessing()
      recipient.forward(query)
    }
    case ProcessBatch => sendBatchForProcessing()
    case message => recipient.forward(message)
  }

  private def sendBatchForProcessing(): Unit = {
    log.info(s"Processing batch, currentNumberOfEvents=${currentNumberOfEvents}")

    currentTick.map(_.cancel())
    currentTick = None

    if (currentNumberOfEvents > 0) {
      recipient ! BatchedEventEnvelopes(bufferedEvents.take(currentNumberOfEvents))
      currentNumberOfEvents = 0
    }
  }
}
