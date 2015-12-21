/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import akka.actor._
import akka.event.LoggingReceive

import scala.concurrent.duration._

private[cqrs] object JournalReader {
  case object ReplayAllEvents
  case object AllEventsHaveBeenReplayed
  case object ReplyFailed

  def props: Props = {
    Props[JournalReader]
  }
}

/**
 * INTERNAL API.
 * 
 * Reads all ids from Cassandra storage and instantiates [[PersistentActorReplayer]] for each persistent actor. 
 * 
 * When all events are replayed this actor will stop itself and send to the issuer of replay process [[cqrs.query.JournalReader.AllEventsHaveBeenReplayed]]
 * 
 * Important note: use JournalReader only for one replay at the time!
 * Do not share instance of Journal reader between different entities that want to get replayed events.
 * Also do not spawn two replays at the same time.
 */
private[cqrs] class JournalReader extends Actor with ActorLogging {
  implicit val system = context.system

  var issuer: Option[ActorRef] = None
  var numberOfProcessors = 0
  var processorsDone = 0

  def receive = receiveRequest

  private lazy val reader = new CassandraJournalPersistenceIdsReader

  private def startReplayOfSingleProcessor(id: String) = {
    val child = context.actorOf(
      PersistentActorReplayer(id, messagesTo = issuer.get, acksTo = self))
    context.watch(child)
  }

  import JournalReader._

  def receiveRequest: Receive = LoggingReceive {
    case ReplayAllEvents => {
      issuer = Some(sender)
      log.info(s"Replay of events requested by $sender started")

      val ids = reader.readAll()

      numberOfProcessors = ids.size

      if (numberOfProcessors == 0) {
        log.info("No aggregate to reply. Reply finished.")
        sender ! AllEventsHaveBeenReplayed
      }
      else {
        processorsDone = 0
        for (id <- ids) {
          startReplayOfSingleProcessor(id)
        }

        context.setReceiveTimeout(5.minutes)
        context.become(receiveAcks)

        log.info(s"Spawned ${ids.size} replay processes")
      }
    }
  }

  def receiveAcks: Receive = LoggingReceive {
    case AllEventsHaveBeenReplayed => {
      processorsDone += 1
      context.unwatch(sender())
      context.stop(sender())
      log.debug("Aggregate finished reply.")
      if (processorsDone == numberOfProcessors) {
        issuer.get ! AllEventsHaveBeenReplayed
        context.become(receiveRequest)
        log.info("All events have been replayed. Reply finished.")
      }
    }
    case ReceiveTimeout => {
      log.error(s"Process of reading the journal has timed out (received from $sender)." +
        s" Replayed $processorsDone / $numberOfProcessors aggregates.")

      issuer.get ! ReplyFailed
      context.stop(self)
    }
    case Terminated(actor) => {
      log.error(s"Child $actor was terminated during the reply.")
      issuer.get ! ReplyFailed
      context.stop(self)

      // Probably we can do it better, e.g. restart the child.
      // The problem is that in this case we can duplicate messages.
      // I would assume for now we do not want that and we can live
      // with simple crash in this case for now.
    }
  }
}
