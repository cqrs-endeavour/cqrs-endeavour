/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import java.util.UUID

import akka.testkit.TestProbe
import cqrs.query.JournalReader._
import cqrs.test.entities.TestPersistentActor
import cqrs.test.utils.CassandraIsolatedSpec

class PersistentActorReplayerSpec extends CassandraIsolatedSpec {

  import TestPersistentActor._

  "PersistentActorReplayer" should "replay all events in order" in {
    // given
    val actorId = s"id123-${UUID.randomUUID()}"
    val actor = system.actorOf(TestPersistentActor.props(actorId))

    actor ! TestCommand(actorId, 2, "first")
    actor ! TestCommand(actorId, 5, "second")
    actor ! TestCommand(actorId, 20, "third")

    // wait for confirmation that it was persisted
    expectMsg(PersistedAck)
    expectMsg(PersistedAck)
    expectMsg(PersistedAck)

    val messageReceiver = TestProbe()
    val ackReceiver = TestProbe()

    // when
    system.actorOf(PersistentActorReplayer(
      actorId, messageReceiver.ref, ackReceiver.ref))

    // then
    messageReceiver.expectMsg(EventEnvelope(actorId, 1, TestEvent(actorId, 2, "first")))
    messageReceiver.expectMsg(EventEnvelope(actorId, 2, TestEvent(actorId, 5, "second")))
    messageReceiver.expectMsg(EventEnvelope(actorId, 3, TestEvent(actorId, 20, "third")))
    messageReceiver.expectNoMsg()
    ackReceiver.expectMsg(AllEventsHaveBeenReplayed)
    ackReceiver.expectNoMsg()
    stopAndCleanPersistentActors(actor)
  }

  it should "replay two different actors" in {
    // given
    val actor1Id = s"id222-${UUID.randomUUID()}"
    val actor2Id = s"id333-${UUID.randomUUID()}"
    val actor1 = system.actorOf(TestPersistentActor.props(actor1Id))
    val actor2 = system.actorOf(TestPersistentActor.props(actor2Id))

    actor1 ! TestCommand(actor1Id, 2, "first")
    actor2 ! TestCommand(actor2Id, 5, "second")

    // wait for confirmation that it was persisted
    expectMsg(PersistedAck)
    expectMsg(PersistedAck)

    val messageReceiver1 = TestProbe()
    val ackReceiver1 = TestProbe()
    val messageReceiver2 = TestProbe()
    val ackReceiver2 = TestProbe()

    // when
    system.actorOf(PersistentActorReplayer(
      actor1Id, messageReceiver1.ref, ackReceiver1.ref))
    system.actorOf(PersistentActorReplayer(
      actor2Id, messageReceiver2.ref, ackReceiver2.ref))

    // then
    messageReceiver1.expectMsg(EventEnvelope(actor1Id, 1, TestEvent(actor1Id, 2, "first")))
    messageReceiver2.expectMsg(EventEnvelope(actor2Id, 1, TestEvent(actor2Id, 5, "second")))
    messageReceiver1.expectNoMsg()
    messageReceiver2.expectNoMsg()
    ackReceiver1.expectMsg(AllEventsHaveBeenReplayed)
    ackReceiver2.expectMsg(AllEventsHaveBeenReplayed)
    ackReceiver1.expectNoMsg()
    ackReceiver2.expectNoMsg()
    stopAndCleanPersistentActors(actor1, actor2)
  }
}
