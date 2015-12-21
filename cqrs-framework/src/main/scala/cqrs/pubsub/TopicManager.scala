/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import akka.actor.ActorRef

/**
 * Manages subscriptions and topics for publish/subscribe mechanism purposes.
 */
private[pubsub] class TopicManager {
  private var messages: Vector[Any] = Vector.empty
  private var subscribers: Vector[ActorRef] = Vector.empty

  def addSubscriberAndReplay(subscriber: ActorRef): Unit = {
    subscribers = subscribers :+ subscriber
    messages.foreach(subscriber ! _)
  }

  def addMessage(message: Any): Unit = {
    subscribers.foreach(_ ! message)
    messages = messages :+ message
  }
}