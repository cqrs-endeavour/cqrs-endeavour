/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.testkit

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ Actor, ActorRef }
import cqrs.command.DomainEvent
import cqrs.query.EventEnvelopeBatcher.BatchedEventEnvelopes
import cqrs.query.{ DomainQuery, EventEnvelope }

object TestDomainViewRef {
  implicit def toActorRef(testDomainViewRef: TestDomainViewRef): ActorRef = {
    testDomainViewRef.viewActorRef
  }
}

/**
 * Wrapper around actor around [[cqrs.query.DomainView]] actor for a little better type safety, also 
 * makes it easier to send batches and [[DomainEvent]] instances that have to be wrapped in [[EventEnvelope]]. 
 */
case class TestDomainViewRef(viewActorRef: ActorRef, persistenceId: String = "TestDomainViewRef") {

  private val counter = new AtomicInteger()

  def sendQuery(query: DomainQuery)(implicit sender: ActorRef = Actor.noSender): Unit = {
    viewActorRef ! query
  }

  def sendEvent(event: DomainEvent)(implicit sender: ActorRef = Actor.noSender): Unit = {
    viewActorRef ! toEnvelope(event)
  }

  def sendEvents(events: Seq[DomainEvent])(implicit sender: ActorRef = Actor.noSender): Unit = {
    events.foreach(sendEvent)
  }

  def sendBatch(events: Seq[DomainEvent])(implicit sender: ActorRef = Actor.noSender): Unit = {
    val batch = toBatch(events)

    viewActorRef ! batch
  }

  def sendMessage(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit = {
    viewActorRef ! msg
  }

  private def toBatch(events: Seq[DomainEvent]): BatchedEventEnvelopes = {
    val envelopes = events.map(toEnvelope)

    BatchedEventEnvelopes(envelopes)
  }

  private def toEnvelope(event: DomainEvent): EventEnvelope = {
    EventEnvelope(persistenceId, counter.incrementAndGet(), event)
  }
}
