/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import akka.actor.{ ActorLogging, ActorRef, Props }
import akka.persistence.{PersistentActor, RecoveryCompleted}
import cqrs.command.DomainEvent

private[cqrs] class PersistentActorReplayer(val persistenceId: String, messagesTo: ActorRef, acksTo: ActorRef)
    extends PersistentActor with ActorLogging {

  import cqrs.query.JournalReader._

  override def preStart(): Unit = {
    log.info(s"Starting recovery of persistenceId = $persistenceId.")
  }

  override def receiveRecover: Receive = {
    case RecoveryCompleted => {
      log.info(s"All events for persistenceId = $persistenceId has been replayed.")
      acksTo ! AllEventsHaveBeenReplayed
    }
    case event: DomainEvent => {
      val envelope = EventEnvelope(persistenceId, lastSequenceNr, event)
      log.debug(s"Replayed event from the journal: $envelope")
      messagesTo ! envelope
    }
  }

  override def receiveCommand: Receive = {
    case command => {
      log.error(s"cannot receive commands, got: $command")
    }
  }
}

object PersistentActorReplayer {
  def apply(persistenceId: String, messagesTo: ActorRef, acksTo: ActorRef): Props = {
    Props(classOf[PersistentActorReplayer], persistenceId, messagesTo, acksTo)
  }
}