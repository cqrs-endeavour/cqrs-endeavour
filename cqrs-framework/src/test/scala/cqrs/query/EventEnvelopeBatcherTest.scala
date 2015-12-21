/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import cqrs.STActorSpec
import cqrs.command.DomainEvent
import cqrs.query.EventEnvelopeBatcher.BatchedEventEnvelopes
import cqrs.query.EventEnvelopeBatcherTest.{ TestDomainQuery, Envelope }

import scala.concurrent.duration._

object EventEnvelopeBatcherTest {
  case object TestDomainEvent extends DomainEvent
  case object TestDomainQuery extends DomainQuery

  val Envelope = EventEnvelope("persistenceId", 1, TestDomainEvent)
}

class EventEnvelopeBatcherTest extends STActorSpec(ConfigFactory.load())  {

  "EventEnvelopeBatcher" should "flush envelopes when limit is reached even if flush interval has not yet passed" in {
      // given
      val testRecipient = TestProbe()
      val batcher = system.actorOf(EventEnvelopeBatcher.props(5, 1 hour, testRecipient.ref))

      // when
      batcher ! Envelope
      batcher ! Envelope
      batcher ! Envelope
      batcher ! Envelope
      batcher ! Envelope

      // then
      val batchedEnvelopes = testRecipient.expectMsgClass(classOf[BatchedEventEnvelopes])
      assert(batchedEnvelopes.envelopes.size == 5)
    }

    it should "flush envelopes if flush interval has passed" in {
      // given
      val testRecipient = TestProbe()
      val batcher = system.actorOf(EventEnvelopeBatcher.props(500, 1 seconds, testRecipient.ref))

      // when
      batcher ! Envelope
      batcher ! Envelope
      batcher ! Envelope
      batcher ! Envelope
      batcher ! Envelope

      // then
      val batchedEnvelopes = testRecipient.expectMsgClass(classOf[BatchedEventEnvelopes])
      assert(batchedEnvelopes.envelopes.size > 0)
    }

    it should "forward any message that is not DomainEvent to recipient" in {
      // given
      val testRecipient = TestProbe()
      val batcher = system.actorOf(EventEnvelopeBatcher.props(500, 1 hour, testRecipient.ref))

      // when
      batcher ! "firstMessage"
      batcher ! "secondMessage"

      // then
      testRecipient.expectMsg("firstMessage")
      testRecipient.reply("firstReply")

      testRecipient.expectMsg("secondMessage")
      testRecipient.reply("secondReply")

      expectMsg("firstReply")
      expectMsg("secondReply")
    }

    it should "forward DomainQuery to recipient" in {
      // given
      val testRecipient = TestProbe()
      val batcher = system.actorOf(EventEnvelopeBatcher.props(500, 1 hour, testRecipient.ref))

      // when
      batcher ! TestDomainQuery
      batcher ! TestDomainQuery

      // then
      testRecipient.expectMsg(TestDomainQuery)
      testRecipient.reply("firstReply")

      testRecipient.expectMsg(TestDomainQuery)
      testRecipient.reply("secondReply")

      expectMsg("firstReply")
      expectMsg("secondReply")
    }

    it should "flush envelopes before forwarding query" in {
      // given
      val testRecipient = TestProbe()
      val batcher = system.actorOf(EventEnvelopeBatcher.props(500, 1 hour, testRecipient.ref))

      // when
      batcher ! Envelope
      batcher ! Envelope
      batcher ! TestDomainQuery

      // then
      val batchedEnvelopes = testRecipient.expectMsgClass(classOf[BatchedEventEnvelopes])
      testRecipient.expectMsg(TestDomainQuery)
      testRecipient.reply("reply")

      expectMsg("reply")
      assert(batchedEnvelopes.envelopes.size == 2)
    }
}