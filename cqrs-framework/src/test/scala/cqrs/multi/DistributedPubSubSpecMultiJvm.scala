/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.multi

import akka.testkit.TestProbe
import cqrs.command.DomainEvent
import cqrs.pubsub.ReplayablePubSub.{ Subscribe, Publish }
import cqrs.pubsub.ReplayablePubSubExtension
import cqrs.query.EventEnvelope
import cqrs.utils.{ DistributedPubSubConfig, ClusterSpec }

import scala.concurrent.duration._

class DistributedPubSubSpecMultiJvmNode1 extends DistributedPubSubSpec
class DistributedPubSubSpecMultiJvmNode2 extends DistributedPubSubSpec

object DistributedPubSubSpec {
  case class TestEvent(message: String) extends DomainEvent
}

abstract class DistributedPubSubSpec extends ClusterSpec(DistributedPubSubConfig) {
  "DistributedPubSub" should {
    "wait for startup" in {
      joinCluster()
      enterBarrier("startup")
    }

    "subscribe from scratch and distribute messages on different JVMs" in {
      // given
      val pubSub = ReplayablePubSubExtension(system).pubSub
      val probe = TestProbe()
      val topic = "test-topic"
      val node1FirstMessage = EventEnvelope("probe1", 1, DistributedPubSubSpec.TestEvent("node1FirstMessage"))
      val node2FirstMessage = EventEnvelope("probe2", 1, DistributedPubSubSpec.TestEvent("node2FirstMessage"))
      val node1SecondMessage = EventEnvelope("probe1", 2, DistributedPubSubSpec.TestEvent("node1SecondMessage"))

      waitDuration(5.seconds, "distributed-pub-sub initialization")

      runOn(DistributedPubSubConfig.node1) {
        pubSub ! Publish(topic, node1FirstMessage)
      }
      runOn(DistributedPubSubConfig.node2) {
        pubSub ! Publish(topic, node2FirstMessage)
      }
      enterBarrier("initial messages published")

      // when-then
      probe.send(pubSub, Subscribe(topic))

      val firstMessage = probe.expectMsgClass(classOf[EventEnvelope])
      val secondMessage = probe.expectMsgClass(classOf[EventEnvelope])
      val messages = Set(firstMessage, secondMessage)

      messages should contain allOf (node1FirstMessage, node2FirstMessage)

      runOn(DistributedPubSubConfig.node1) {
        pubSub ! Publish(topic, node1SecondMessage)
      }
      enterBarrier("after sending node1 second message")
      val publishedMessage = probe.expectMsgClass(classOf[EventEnvelope])
      publishedMessage should equal(node1SecondMessage)
    }
  }
}