/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import akka.actor._
import cqrs.pubsub.ReplayablePubSubExtension
import cqrs.pubsub.ReplayablePubSub.Subscribe
import JournalReader.{ AllEventsHaveBeenReplayed, ReplayAllEvents, ReplyFailed }
import cqrs.query.CQRSViewEventBus.MostOfTheEventsAlreadyReplayed

/**
 * Abstraction that can be used to subscribe for [[EventEnvelope]] instances from persistent storage and/or event bus (e.g. Kafka).
 * 
 * Clients have to be careful because they may receive duplicate events. For recipients that are not idempotent some kind of
 * event duplication logic has to be implemented, e.g. using [[EventDuplicatesFilter]] that uses 
 * [[EventEnvelope.persistenceId]] and [[EventEnvelope.sequenceNr]] to distinguish between duplicates.
 * 
 * Current implementation may not work correctly when number of events is very large due eager reading of all events 
 * that are stored in Cassandra storage.
 * 
 * @param view recipient of [[EventEnvelope]] messages
 * @param subscribeFromScratch if false [[EventEnvelope]]s will be read only from Kafka.
 */
class CQRSViewEventBus(view: ActorRef, subscribeFromScratch: Boolean) extends Actor with ActorLogging {
  private val pubSubTopic = context.system.settings.config.getString("cqrs.pub-sub-topic")

  private val pubSub = ReplayablePubSubExtension(context.system).pubSub
  private val reader = context.actorOf(JournalReader.props)

  override def receive: Receive = journalReplyReceive

  override def preStart(): Unit = {
    if (!subscribeFromScratch) {
      subscribeToEventBus()
    }
    else {
      reader ! ReplayAllEvents
    }
    super.preStart()
  }

  def pubSubAndQueryReceive: Receive = {
    case envelope: EventEnvelope => {
      view ! envelope
    }
  }

  def journalReplyReceive: Receive = {
    case envelope: EventEnvelope => {
      log.debug(s"replaying event: ${envelope}")
      view ! envelope
    }
    case AllEventsHaveBeenReplayed => {
      log.info("replay has finished successfully")
      subscribeToEventBus()
    }
    case ReplyFailed => {
      log.error("Journal reply failed")
      context.stop(self)
    }
  }

  private def subscribeToEventBus(): Unit = {
    view ! MostOfTheEventsAlreadyReplayed
    context.become(pubSubAndQueryReceive)
    pubSub ! Subscribe(pubSubTopic)
  }
}

object CQRSViewEventBus {
  def apply(view: ActorRef, subscribeFromScratch: Boolean): Props = {
    Props(classOf[CQRSViewEventBus], view, subscribeFromScratch)
  }

  object MostOfTheEventsAlreadyReplayed
}
