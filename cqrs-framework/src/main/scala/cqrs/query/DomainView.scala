/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import akka.actor.{ ActorLogging, Actor }
import akka.actor.Status.Failure
import akka.event.LoggingReceive
import cqrs.query.EventEnvelopeBatcher.BatchedEventEnvelopes

/**
 * Basic abstraction of read side.
 */
trait DomainView extends Actor with ActorLogging {
  def receiveEvent: PartialFunction[EventEnvelope, Unit]
  def receiveQuery: PartialFunction[DomainQuery, Any]

  /**
   * Receive-like function that can be overridden if concrete view has to handle messages different than 
   * subclasses of [[DomainQuery]], [[EventEnvelope]], [[BatchedEventEnvelopes]].
   * 
   * If this method can't handle different message [[Failure]] with [[IllegalArgumentException]] will be sent to sender
   */
  def receiveMessage: PartialFunction[Any, Unit] = Map.empty

  /**
   * Optional callback that should be overridden if view prefers to process messages in batches,
   * e.g. in case of transaction processing performance.
   * 
   * By default events will be processed one-by-one.
   */
  def receiveBatch(envelopes: Seq[EventEnvelope]) = {
    log.error("DomainView received batch but it has no support for batch processing, falling back to sequential processing")
    envelopes.foreach(receiveEvent(_))
  }

  def receive = LoggingReceive {
    case envelope: EventEnvelope => {
      receiveEvent(envelope)
    }
    case BatchedEventEnvelopes(envelopes) => {
      receiveBatch(envelopes)
    }
    case query: DomainQuery => {
      if (receiveQuery.isDefinedAt(query)) {
        val result = receiveQuery(query)
        sender ! result
      }
      else {
        val exception = new IllegalArgumentException(s"invalid query: $query")
        log.error(exception, "got invalid query")
        sender ! Failure(exception)
      }
    }
    case differentMsg => {
      if (receiveMessage.isDefinedAt(differentMsg)) {
        log.info(s"got msg: $differentMsg")
        receiveMessage(differentMsg)
      }
      else {
        val exception = new IllegalArgumentException(s"invalid message: $differentMsg")
        log.error(exception, "got invalid message")
        sender ! Failure(exception)
      }
    }
  }
}
