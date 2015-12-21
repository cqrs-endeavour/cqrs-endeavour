/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.repository

import java.util.UUID

import akka.cluster.sharding.ShardRegion

import cqrs.command.{ AggregateQuery, CreateDomainCommand, CreateDomainCommandEnvelope, ModifyDomainCommand }

object ClusterSharding {
  /**
   * Extracts aggregate root ids from [[CreateDomainCommand]], [[ModifyDomainCommand]] and [[AggregateQuery]] instances.
   */
  val domainCommandIdExtractor: ShardRegion.ExtractEntityId = {
    case command: ModifyDomainCommand => (command.aggregateRootId.toString, command)
    case command: CreateDomainCommand => {
      val messageToSend = CreateDomainCommandEnvelope(command.id, command)

      (command.id.toString, messageToSend)
    }
    case query: AggregateQuery => (query.aggregateRootId.toString, query)
  }

  private def calculateShard(numberOfNodes: Long, id: UUID): String = {
    // guard against negative modulus value - in Java/Scala modulo operation can return negative values
    val modulus = math.abs(id.getMostSignificantBits % numberOfNodes)

    modulus.toString
  }
  
  /**
   * Calculates shardId from [[ModifyDomainCommand]], [[CreateDomainCommand]], [[AggregateQuery]], [[CreateDomainCommandEnvelope]].
   *
   * @param numberOfNodes should be around a factor of ten greater than planned max number of nodes, see
   *                      https://groups.google.com/forum/#!topic/akka-user/Jrzuf6KUga4 and
   *                      http://doc.akka.io/docs/akka/2.4.0/scala/cluster-sharding.html#An_Example
   */
  def domainCommandShardResolver(numberOfNodes: Long): ShardRegion.ExtractShardId = {
    case command: ModifyDomainCommand => calculateShard(numberOfNodes, command.aggregateRootId)

    case command: CreateDomainCommand => calculateShard(numberOfNodes, command.id)

    case query: AggregateQuery => calculateShard(numberOfNodes, query.aggregateRootId)

    case CreateDomainCommandEnvelope(id, cmd) => calculateShard(numberOfNodes, id)
  }
}
