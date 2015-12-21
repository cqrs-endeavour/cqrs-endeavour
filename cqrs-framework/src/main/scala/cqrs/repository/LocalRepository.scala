/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.repository

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.cluster.sharding.ShardRegion.Passivate
import akka.event.LoggingReceive
import cqrs.command.{ AggregateQuery, CreateDomainCommand, ModifyDomainCommand }

import scala.reflect.ClassTag

/**
 * Base class for local repository-like actors.
 * 
 * This actor is responsible for creating and/or forwarding messages to [[cqrs.command.AggregateRootActor]]s.
 * 
 * Also supports passivation of [[cqrs.command.AggregateRootActor]] instances just like [[ClusterSharding]]
 */
abstract class LocalRepository[CreateCmd <: CreateDomainCommand: ClassTag, ModifyCmd <: ModifyDomainCommand: ClassTag]
    extends Actor with ActorLogging {

  private def aggregateRootOfId(id: UUID): ActorRef = {
    context.child(id.toString).getOrElse {
      context.actorOf(aggregateRootProps, id.toString)
    }
  }

  private val createCmdClass = implicitly[ClassTag[CreateCmd]].runtimeClass
  private val modifyCmdClass = implicitly[ClassTag[ModifyCmd]].runtimeClass

  /**
   * @return instance of [[Props]] that will be used to construct [[cqrs.command.AggregateRootActor]]
   */
  def aggregateRootProps: Props

  def receive: Receive = LoggingReceive {

    case cmd: CreateDomainCommand if createCmdClass.isInstance(cmd) => {
      aggregateRootOfId(cmd.id) forward cmd
    }

    case cmd: ModifyDomainCommand if modifyCmdClass.isInstance(cmd) => {
      aggregateRootOfId(cmd.aggregateRootId) forward cmd
    }

    case query: AggregateQuery => {
      aggregateRootOfId(query.aggregateRootId) forward query
    }

    case passivate: Passivate => {
      log.info(s"Passivating ${sender()}")
      sender() ! passivate.stopMessage
    }
  }
}
