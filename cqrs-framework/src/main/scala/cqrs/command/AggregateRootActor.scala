/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID

import akka.actor.{ ReceiveTimeout, ActorLogging, ActorRef }
import akka.cluster.sharding.ShardRegion.Passivate

import akka.event.LoggingReceive
import akka.persistence.{ PersistentActor, SnapshotOffer }
import cqrs.command.AggregateRootActor.{ PassivateAggregateRoot, SaveSnapshot }
import cqrs.pubsub.ReplayablePubSubExtension
import cqrs.pubsub.ReplayablePubSub.PublishWithPartitioning
import cqrs.query.EventEnvelope
import scala.concurrent.duration._

/**
 * Operational interface of the actor.
 */
object AggregateRootActor {

  /**
   * Requests a snapshot of the aggregate state to be persisted. That makes the replay faster.
   */
  case object SaveSnapshot

  /**
   * Destroys the in-memory representation of the aggregate. When a next request comes, the aggregate need to be
   * recreated from the events in the event store.
   */
  case object PassivateAggregateRoot
}

/**
 * An actor responsible for a single aggregate instance.
 *
 * Receives requests from the user using Akka messaging, provides serial processing of requests, gives the access to the
 * event bus to publish events and replays the state of the aggregate when created based on the published events saved
 * in the events store.
 */
trait AggregateRootActor[AR <: AggregateRoot[AR]] extends PersistentActor with ActorLogging with EventBus {
  private val pubSub = ReplayablePubSubExtension(context.system).pubSub
  private val pubSubTopic = context.system.settings.config.getString("cqrs.pub-sub-topic")
  private val aggregateRootState = new AggregateRootActorState[AR](id, this, aggregateRootFactory)

  override def persistenceId = self.path.name
  def id = UUID.fromString(persistenceId)

  context.setReceiveTimeout(45.seconds)

  // TODO - refactor
  def eventPostProcess: PartialFunction[DomainEvent, Unit] = {
    case _ =>
  }

  def publish(event: DomainEvent): Unit = {
    persist(event) { event => publishAfterPersist(event, sender()) }
  }

  def aggregateRootFactory: AggregateRootFactory[AR]

  private def publishAfterPersist(event: DomainEvent, commandSender: ActorRef) = {
    aggregateRootState.updateAggregateRoot(event)
    pubSub ! PublishWithPartitioning(pubSubTopic, EventEnvelope(id.toString, lastSequenceNr, event), id)
    eventPostProcess(event)
    commandSender ! event
  }

  val receiveRecover: Receive = LoggingReceive {
    case SnapshotOffer(_, snapshot: AggregateRoot[AR]) => aggregateRootState.acceptSnapshotOffer(snapshot)
    case event: DomainEvent => aggregateRootState.updateAggregateRoot(event)
  }

  val receiveCommand: Receive = LoggingReceive {
    case cmd: DomainCommand => {
      try {
        aggregateRootState.handleCommand(cmd)
      }
      catch {
        case ex: Exception => respondWithException(ex)
      }
    }
    case CreateDomainCommandEnvelope(envelopeId, cmd) => {
      if (envelopeId == id) {
        receiveCommand(cmd)
      }
      else {
        respondWithException(new IllegalCommandException(cmd, s"$cmd was addressed to $envelopeId not $id"))
      }
    }
    case query: AggregateQuery => {
      try {
        sender ! aggregateRootState.handleQuery(query)
      }
      catch {
        case ex: Exception => respondWithException(ex)
      }
    }

    case SaveSnapshot => saveSnapshot(aggregateRootState.aggregateRoot)

    case ReceiveTimeout => {
      context.parent ! Passivate(stopMessage = PassivateAggregateRoot)
    }
    case PassivateAggregateRoot => context.stop(self)
  }

  private def respondWithException(ex: Exception): Unit = {
    log.error(ex, s"rotation-$id")

    sender ! akka.actor.Status.Failure(ex)
  }
}