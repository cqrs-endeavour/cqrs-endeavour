/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.repository

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.sharding.ClusterShardingSettings
import cqrs.command.AggregateRootActor
import cqrs.repository

import scala.reflect.ClassTag

/**
 * Factory class for constructing repository-like actor that is responsible for forwarding [[cqrs.command.DomainCommand]] 
 * instances to proper [[AggregateRootActor]]s. When [[AggregateRootActor]] is not present in memory repository actor
 * is responsible for creating it.
 * @tparam T type of [[AggregateRootActor]]
 */
abstract class RepositoryProvider[T <: AggregateRootActor[_]: ClassTag] {

  private val clazz = implicitly[ClassTag[T]].runtimeClass

  private val aggregateRootName = clazz.getSimpleName.toLowerCase

  /**
   * @return [[Props]] instance that will be used to create local (not sharded) repository actor
   */
  protected def localRepositoryProps: Props

  /**
   * @return [[Props]] instance that will be used to construct concrete class of [[AggregateRootActor]]
   */
  protected def aggregateRootProps: Props

  /**
   * @param system actorSystem in context of which repository actor will be created. Configuration will also be fetched from
   *               this [[ActorSystem]] instance to determine the type of repository.
   * @return either actor subclassing [[LocalRepository]] or instance of [[ClusterSharding]] for clustered repository
   */
  def provide(implicit system: ActorSystem): ActorRef = {
    val repositoryType = system.settings.config.getString("cqrs.repository-type")
    val numberOfNodes = system.settings.config.getLong("cqrs.number-of-nodes")

    system.log.info(s"Creating ${repositoryType} repository")

    repositoryType match {
      case "local" => system.actorOf(localRepositoryProps, s"${aggregateRootName}-repository")
      case "sharded" => {
        val settings = ClusterShardingSettings
        akka.cluster.sharding.ClusterSharding(system).start(
          typeName = aggregateRootName,
          entityProps = aggregateRootProps,
          settings = ClusterShardingSettings(system),
          extractEntityId = repository.ClusterSharding.domainCommandIdExtractor,
          extractShardId = repository.ClusterSharding.domainCommandShardResolver(numberOfNodes)
        )
      }
    }
  }
}
