/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import java.util.UUID

import akka.actor.ReceiveTimeout
import cqrs.query.JournalReader._
import cqrs.test.entities.TestPersistentActor
import cqrs.test.utils.CassandraIsolatedSpec

class JournalReaderSpec extends CassandraIsolatedSpec {
  import TestPersistentActor._

  "JournalReader" should "replay messages from single persistence id in order" in {
      // given
      val actorId = s"id1-${UUID.randomUUID()}"
      val actor = system.actorOf(TestPersistentActor.props(actorId))

      actor ! TestCommand(actorId, 2, "first")
      actor ! TestCommand(actorId, 5, "second")
      actor ! TestCommand(actorId, 20, "third")

      // wait for confirmation that it was persisted
      expectMsg(PersistedAck)
      expectMsg(PersistedAck)
      expectMsg(PersistedAck)

      val reader = system.actorOf(JournalReader.props)

      // when
      reader ! ReplayAllEvents

      // then
      expectMsg(EventEnvelope(actorId, 1, TestEvent(actorId, 2, "first")))
      expectMsg(EventEnvelope(actorId, 2, TestEvent(actorId, 5, "second")))
      expectMsg(EventEnvelope(actorId, 3, TestEvent(actorId, 20, "third")))
      expectMsg(AllEventsHaveBeenReplayed)
      expectNoMsg()

      stopAndCleanPersistentActors(actor)
    }

    it should "replay messages from many persistence ids of the same type in partial order" in {
      val actor1Id = "id3"
      val actor2Id = "id4"
      val actor1 = system.actorOf(TestPersistentActor.props(actor1Id))
      val actor2 = system.actorOf(TestPersistentActor.props(actor2Id))

      val cmd1 = TestCommand(actor1Id, 11, "eleven")
      val cmd2 = TestCommand(actor2Id, 12, "twelve")
      val cmd3 = TestCommand(actor1Id, 13, "thirteen")
      val cmd4 = TestCommand(actor2Id, 14, "fourteen")

      actor1 ! cmd1
      actor2 ! cmd2
      actor1 ! cmd3
      actor2 ! cmd4

      // wait for confirmation that it was persisted
      expectMsg(PersistedAck)
      expectMsg(PersistedAck)
      expectMsg(PersistedAck)
      expectMsg(PersistedAck)

      val reader = system.actorOf(JournalReader.props)

      // when
      reader ! ReplayAllEvents

      // then
      val wrappedEvents = receiveN(4)
      val event1 = TestEvent(actor1Id, 11, "eleven")
      val event2 = TestEvent(actor2Id, 12, "twelve")
      val event3 = TestEvent(actor1Id, 13, "thirteen")
      val event4 = TestEvent(actor2Id, 14, "fourteen")

      val events = wrappedEvents.map(_.asInstanceOf[EventEnvelope].event)
      assert(events.toSet == Set(event1, event2, event3, event4))
      assert(events.indexOf(event1) < events.indexOf(event3))
      assert(events.indexOf(event2) < events.indexOf(event4))

      expectMsg(AllEventsHaveBeenReplayed)
      expectNoMsg()

      stopAndCleanPersistentActors(actor1, actor2)
    }

    it should "replay no messages when there is no persistent actor" in {
      // given
      val reader = system.actorOf(JournalReader.props)

      // when
      reader ! ReplayAllEvents

      // then
      expectMsg(AllEventsHaveBeenReplayed)
      expectNoMsg()
    }

    it should "inform about failure when timeouted" in {
      // given
      val actor1Id = s"id5-${UUID.randomUUID()}"
      val actor1 = system.actorOf(TestPersistentActor.props(actor1Id))
      actor1 ! TestCommand(actor1Id, 11, "eleven")

      // wait for confirmation that it was persisted
      expectMsg(PersistedAck)

      val reader = system.actorOf(JournalReader.props)

      // when
      reader ! ReplayAllEvents
      reader ! ReceiveTimeout

      // then
      fishForMessage() {
        case ReplyFailed => true
      }
    }
}

