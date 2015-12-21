/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import java.util.UUID

import akka.actor._
import akka.event.LoggingReceive
import cqrs.query.EventEnvelope
import cqrs.pubsub.ReplayablePubSub.{ PublishWithPartitioning, Publish, Subscribe }

/**
 * Interface of the publish subscribe mechanism with a durable subscription pattern.
 */
private[cqrs] trait ReplayablePubSub extends Actor {
  def subscribeFromScratch(topic: String, subscriber: ActorRef): Unit
  def publish(topic: String, message: EventEnvelope): Unit

  def publishWithPartitioning(topic: String, message: EventEnvelope, key: UUID): Unit = {
    // by default normal publish action
    publish(topic, message)
  }

  def receive: Receive = LoggingReceive {
    case Subscribe(topic) => {
      subscribeFromScratch(topic, sender())
    }
    case Publish(topic, message) => {
      publish(topic, message)
    }
    case PublishWithPartitioning(topic, message, key) => {
      publishWithPartitioning(topic, message, key)
    }
  }
}

private[cqrs] object ReplayablePubSub {
  case class Subscribe(topic: String)
  case class Publish(topic: String, message: EventEnvelope)
  case class PublishWithPartitioning(topic: String, message: EventEnvelope, key: UUID)
}