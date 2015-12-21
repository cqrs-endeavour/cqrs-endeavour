/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import akka.actor.ActorRef

/**
 * Implements the durable subscription pattern in memory only. Stores all events that were ever published.
 */
trait InMemoryDurableSubscriber {
  type SubscribersRegistry = Map[String, TopicManager]

  private var topicManagers: SubscribersRegistry = Map.empty

  protected def addMessageToTopic(topic: String, message: Any): Unit = {
    val manager = topicManager(topic)

    manager.addMessage(message)
  }

  protected def addNewSubscriberToTopicAndReplay(topic: String, subscriber: ActorRef): Unit = {
    val manager = topicManager(topic)

    manager.addSubscriberAndReplay(subscriber)
  }

  private def topicManager(topic: String): TopicManager = {
    val topicManager = topicManagers.get(topic)

    topicManager getOrElse addTopicManager(topic)
  }

  private def addTopicManager(topic: String): TopicManager = {
    val topicManager = new TopicManager
    topicManagers = topicManagers.updated(topic, topicManager)
    topicManager
  }
}