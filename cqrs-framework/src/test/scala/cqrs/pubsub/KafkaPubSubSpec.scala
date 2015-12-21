/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import java.util.UUID

import akka.actor.Props
import akka.testkit.TestProbe
import cqrs.pubsub.ReplayablePubSub.{Publish, Subscribe}
import cqrs.query.EventEnvelope
import cqrs.test.entities.TestPersistentActor.TestEvent
import cqrs.test.utils.KafkaIsolatedSpec

class KafkaPubSubSpec extends KafkaIsolatedSpec {

  "KafkaPubSub" should "publish and receive messages" in {
    val event = TestEvent("id1", 1, "A")
    val mediator = system.actorOf(Props[KafkaPubSub])

    val topic = UUID.randomUUID().toString

    val probe = TestProbe()
    mediator.tell(Subscribe(topic), probe.ref)

    mediator ! Publish(topic, EventEnvelope("id1", 2, event))
    probe.expectMsg(EventEnvelope("id1", 2, event))
  }

  it should "publish and replay messages from scratch" in {
    val event = TestEvent("id2", 1, "A")
    val mediator = system.actorOf(Props[KafkaPubSub])

    val topic = UUID.randomUUID().toString
    mediator ! Publish(topic, EventEnvelope("id2", 5, event))

    val probe = TestProbe()
    mediator.tell(Subscribe(topic), probe.ref)

    probe.expectMsg(EventEnvelope("id2", 5, event))
  }

  it should "replay messages from last recorded offset" in {
    // given
    val firstEvent = TestEvent("id2", 1, "A")
    val secondEvent = TestEvent("id2", 2, "A")
    val mediator = system.actorOf(Props[KafkaPubSub], "kafka-pub-sub")
    val topic = UUID.randomUUID().toString
    val probe = TestProbe()

    mediator ! Publish(topic, EventEnvelope("id2", 5, firstEvent))
    mediator.tell(Subscribe(topic), probe.ref)
    probe.expectMsg(EventEnvelope("id2", 5, firstEvent))

    stopActor(mediator)

    // when
    val newMediator = system.actorOf(Props[KafkaPubSub], "kafka-pub-sub-2")
    newMediator ! Publish(topic, EventEnvelope("id2", 6, secondEvent))
    newMediator.tell(Subscribe(topic), probe.ref)

    // then
    probe.expectMsg(EventEnvelope("id2", 6, secondEvent))
  }
}
