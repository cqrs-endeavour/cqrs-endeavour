/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import akka.actor.ActorRef
import cqrs.query.EventEnvelope

/**
 * Non-distributed version of the [[InMemoryDurableSubscriber]].
 */
private[cqrs] class SimplePubSub extends ReplayablePubSub with InMemoryDurableSubscriber {
  override def subscribeFromScratch(topic: String, subscriber: ActorRef): Unit = {
    addNewSubscriberToTopicAndReplay(topic, subscriber)
  }

  override def publish(topic: String, message: EventEnvelope): Unit = {
    addMessageToTopic(topic, message)
  }
}

