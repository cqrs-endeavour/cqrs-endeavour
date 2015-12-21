/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.repository

import java.util.UUID

import akka.cluster.sharding.ShardRegion
import cqrs.test.entities.TestAggregateRoot.ModifyTestAggregateRoot
import org.scalatest.FlatSpec

import scala.collection.mutable

abstract class DomainCommandShardResolverScope(nodes: Long, aggregateRoots: Long, percentage: Double) {
  private val countPerNode = aggregateRoots / nodes
  private val offset = (countPerNode * percentage).toLong
  private val minAcceptableValue = countPerNode - offset
  private val maxAcceptableValue = countPerNode + offset

  val acceptableRange = minAcceptableValue to maxAcceptableValue
  val resolver = ClusterSharding.domainCommandShardResolver(nodes)
  val actorsPerShard = mutable.Map[ShardRegion.ShardId, Long]()

  for (_ <- 0L to aggregateRoots) {
    val resolvedShard = resolver(ModifyTestAggregateRoot(UUID.randomUUID(), 1))
    val counter = actorsPerShard.getOrElse(resolvedShard, 0L)
    actorsPerShard.update(resolvedShard, counter + 1)
  }

  val hasAcceptableDistribution = actorsPerShard.forall(elem => acceptableRange.contains(elem._2))
}

class ShardResolverSpec extends FlatSpec {

  "DomainCommandShardResolver" should "have acceptable distribution for 100 000 UUIDs and 10 nodes with 0.1 margin" in new DomainCommandShardResolverScope(10, 100000, 0.1) {
    assert(hasAcceptableDistribution)
  }

  it should "have acceptable distribution for 100 000 UUIDs and 100 nodes with 0.1 margin" in new DomainCommandShardResolverScope(100, 100000, 0.1) {
    assert(hasAcceptableDistribution)
  }

  it should "have acceptable distribution for 100 000 UUIDs and 10 nodes with 0.05 margin" in new DomainCommandShardResolverScope(10, 100000, 0.05) {
    assert(hasAcceptableDistribution)
  }

  it should "have acceptable distribution for 100 000 UUIDs and 100 nodes with 0.05 margin" in new DomainCommandShardResolverScope(100, 100000, 0.05) {
    assert(!hasAcceptableDistribution)
  }
}
