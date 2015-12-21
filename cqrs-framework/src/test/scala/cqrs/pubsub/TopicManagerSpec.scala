/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import akka.testkit.TestProbe
import cqrs.test.utils.DebugActorSpec

class TopicManagerSpec extends DebugActorSpec {
  "TopicManager" should "replay all messages to new subscribers" in {
    // given
    val topicManager = new TopicManager
    val firstMessage = "firstMessage"
    val secondMessage = "secondMessage"

    topicManager.addMessage(firstMessage)
    topicManager.addMessage(secondMessage)

    // when
    topicManager.addSubscriberAndReplay(self)

    // then
    expectMsg(firstMessage)
    expectMsg(secondMessage)
  }

  it should "send new message to all subscribers" in {
    // given
    val topicManager = new TopicManager
    val firstSubscriber = TestProbe()
    val secondSubscriber = TestProbe()
    val message = "message"

    topicManager.addSubscriberAndReplay(firstSubscriber.ref)
    topicManager.addSubscriberAndReplay(secondSubscriber.ref)

    // when
    topicManager.addMessage(message)

    // then
    firstSubscriber.expectMsg(message)
    secondSubscriber.expectMsg(message)
  }
}
