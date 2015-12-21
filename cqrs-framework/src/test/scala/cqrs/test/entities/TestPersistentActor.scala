/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.test.entities

import akka.actor.Props
import akka.event.LoggingReceive
import akka.persistence.{DeleteMessagesFailure, DeleteMessagesSuccess, PersistentActor}
import com.typesafe.scalalogging.slf4j.StrictLogging
import cqrs.command.{DomainCommand, DomainEvent}

object TestPersistentActor {
  def props(id: String): Props = {
    Props(classOf[TestPersistentActor], id)
  }

  case class TestCommand(id: String, someInt: Int, someString: String) extends DomainCommand

  case class TestEvent(id: String, someInt: Int, someString: String) extends DomainEvent

  object PersistedAck

  object RemoveMessages

}

class TestPersistentActor(id: String) extends PersistentActor with StrictLogging {

  import TestPersistentActor._

  override def receiveRecover: Receive = {
    case msg => logger.info(s"Recovered $msg")
  }

  override def receiveCommand: Receive = LoggingReceive {
    case command: TestCommand if command.id == persistenceId => {
      val commandSender = sender()
      logger.info(s"Persisting $command, last seq number is $lastSequenceNr")
      persist(TestEvent(command.id, command.someInt, command.someString))(_ => commandSender ! PersistedAck)
    }
    case RemoveMessages => {
      deleteMessages(lastSequenceNr)
    }
    case DeleteMessagesSuccess(_) => {
      logger.info(s"Successfully removed messages for TestPersistentActor ${self.path}")
      context.stop(self)
    }
    case DeleteMessagesFailure(cause, _) => {
      logger.error(s"There was an error during message deletion for TestPersistentActor ${self.path}", cause)
      context.stop(self)
    }
  }

  override def persistenceId: String = id
}
