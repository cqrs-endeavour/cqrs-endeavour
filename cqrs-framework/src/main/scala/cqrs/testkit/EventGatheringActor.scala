/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.testkit

import akka.actor.Props
import akka.persistence.PersistentActor
import cqrs.command.DomainEvent
import cqrs.testkit.EventGatheringActor.{ ReplayedEvents, GetEvents }

object EventGatheringActor {
  final case class ReplayedEvents(id: String, events: Vector[DomainEvent])

  final case object GetEvents

  def apply(id: String): Props = Props(classOf[EventGatheringActor], id)
}

/**
 * Actor that can be used to replay all events for given aggregate root.
 * 
 * Replayed events can be queried using [[GetEvents]], this actor will replay with [[ReplayedEvents]].
 */
class EventGatheringActor(val id: String) extends PersistentActor {

  var gatheredEvents = Vector[DomainEvent]()

  override def persistenceId = id

  override def receiveRecover: Receive = {
    case event: DomainEvent => gatheredEvents = gatheredEvents :+ event
  }

  override def receiveCommand: Receive = {
    case _ => sender ! akka.actor.Status.Failure(new NotImplementedError)
  }

  override def receive: Receive = {
    case GetEvents => sender ! ReplayedEvents(persistenceId, gatheredEvents)
  }
}
