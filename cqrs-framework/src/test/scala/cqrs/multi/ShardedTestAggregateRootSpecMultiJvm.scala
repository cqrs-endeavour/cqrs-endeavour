/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.multi

import java.util.UUID

import cqrs.pubsub.{ ReplayablePubSub, ReplayablePubSubExtension }
import cqrs.query.EventEnvelope
import cqrs.test.entities.TestAggregateRoot.{ TestAggregateRootCreated, CreateTestAggregateRoot }
import cqrs.test.entities.{ TestAggregateRootRepositoryProvider }
import cqrs.utils.{ ClusterSpec, ShardingClusterConfig }
import scala.concurrent.duration._

class TestAggregateRootShardingSpecMultiJvmNode1 extends TestAggregateRootShardingSpec
class TestAggregateRootShardingSpecMultiJvmNode2 extends TestAggregateRootShardingSpec

trait AggregateRootShardingScope {
  val firstUUID = UUID.fromString("00000000-0000-0000-b1b6-0800200c9a66")
  val secondUUID = UUID.fromString("00000000-0000-0001-b1b6-0800200c9a66")
}

abstract class TestAggregateRootShardingSpec extends ClusterSpec(ShardingClusterConfig) {
  import ShardingClusterConfig._

  "cluster sharded TestAggregateRoot" should {

    "wait for startup" in {
      joinCluster()
      enterBarrier("startup")
    }

    "be distributed between JVMs" in new AggregateRootShardingScope {
      // given
      val repository = TestAggregateRootRepositoryProvider.provide
      val pubSub = ReplayablePubSubExtension(system).pubSub

      // when
      runOn(node1) {
        // when
        repository ! CreateTestAggregateRoot(1, "1")
        repository ! CreateTestAggregateRoot(2, "2")

        expectMsgType[TestAggregateRootCreated]
        expectMsgType[TestAggregateRootCreated]

        enterBarrier("test-aggregate-roots-created")

        // then
        pubSub ! ReplayablePubSub.Subscribe(ShardingClusterConfig.topic)

        val envelope = expectMsgType[EventEnvelope]
        envelope.event shouldBe a[TestAggregateRootCreated]

        expectNoMsg(1.second)
      }

      runOn(node2) {
        // when
        enterBarrier("test-aggregate-roots-created")

        // then
        pubSub ! ReplayablePubSub.Subscribe(ShardingClusterConfig.topic)

        val envelope = expectMsgType[EventEnvelope]
        envelope.event shouldBe a[TestAggregateRootCreated]

        expectNoMsg(1.second)
      }
    }
  }
}