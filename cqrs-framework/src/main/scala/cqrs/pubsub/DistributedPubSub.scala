/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import akka.actor.ActorRef
import akka.cluster.pubsub.{DistributedPubSub => AkkaDistributedPubSub}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}

import cqrs.pubsub.DistributedPubSubSubscriber.{ Envelope, SubscribeAndReplay }
import cqrs.query.EventEnvelope

/**
 * Implements the [[ReplayablePubSub]] using Akka DistributedPubSub extension.
 *
 * Please not that this implementation does not persist events on disk at uses at-least-once delivery semantic. If it is
 * not desirable for your use case, you may want to use [[KafkaPubSub]].
 */
private[cqrs] class DistributedPubSub extends ReplayablePubSub {

  private val globalTopic = "DistributedPubSubGlobalTopic"

  private val mediator = AkkaDistributedPubSub(context.system).mediator

  private val globalTopicSubscriber = context.actorOf(DistributedPubSubSubscriber.props, "global-topic-subscriber")

  mediator ! Subscribe(globalTopic, globalTopicSubscriber)

  override def subscribeFromScratch(topic: String, subscriber: ActorRef): Unit = {
    globalTopicSubscriber ! SubscribeAndReplay(topic, subscriber)
  }

  override def publish(topic: String, message: EventEnvelope): Unit = {
    val envelopedMessage = Envelope(message, topic)
    mediator ! Publish(globalTopic, envelopedMessage)
  }
}
