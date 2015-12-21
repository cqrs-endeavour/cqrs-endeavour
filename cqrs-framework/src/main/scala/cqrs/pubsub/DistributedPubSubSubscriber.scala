/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import akka.actor.{ Actor, Props, ActorRef }
import akka.event.LoggingReceive
import DistributedPubSubSubscriber.{ Envelope, SubscribeAndReplay }

object DistributedPubSubSubscriber {
  case class Envelope(originalMessage: Any, originalTopic: String)

  case class SubscribeAndReplay(topic: String, subscriber: ActorRef)

  def props: Props = Props[DistributedPubSubSubscriber]
}

/**
 * An actor wrapper for InMemoryDurableSubscriber. Required by the DistributedPubSub API.
 */
class DistributedPubSubSubscriber extends Actor with InMemoryDurableSubscriber {

  override def receive: Receive = LoggingReceive {
    case Envelope(message, topic) => {
      addMessageToTopic(topic, message)
    }

    case SubscribeAndReplay(topic, subscriber) => {
      addNewSubscriberToTopicAndReplay(topic, subscriber)
    }
  }
}