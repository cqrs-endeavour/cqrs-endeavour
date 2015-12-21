/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs

import java.util.UUID

import akka.testkit.TestProbe
import cqrs.query.CQRSViewEventBus.MostOfTheEventsAlreadyReplayed
import cqrs.query.{CQRSViewEventBus, EventEnvelope}
import cqrs.test.entities.TestAggregateRoot._
import cqrs.test.entities.TestAggregateRootActor
import cqrs.test.utils.CassandraKafkaIsolatedSpec

class CqrsIntegrationSpec extends CassandraKafkaIsolatedSpec {

  "DomainView" should "receive events from the past and in the real time" in {

    // given
    val id1 = UUID.randomUUID()
    val id2 = UUID.randomUUID()
    val id3 = UUID.randomUUID()

    val aggregate1 = system.actorOf(TestAggregateRootActor.props, id1.toString)
    val aggregate2 = system.actorOf(TestAggregateRootActor.props, id2.toString)

    aggregate1 ! CreateTestAggregateRoot(1, "ar1-1")
    aggregate2 ! CreateTestAggregateRoot(2, "ar2-1")
    aggregate1 ! ModifyTestAggregateRoot(id1, 1)

    receiveN(3) // wait for actual persist operations

    val view = TestProbe()

    // when-then 
    val eventBus = system.actorOf(CQRSViewEventBus(view.ref, subscribeFromScratch = true), "cqrs-view-event-bus")

    // then
    val event1_1 = TestAggregateRootCreated(id1, 1, "ar1-1")
    val event2_1 = TestAggregateRootCreated(id2, 2, "ar2-1")
    val event1_2 = TestAggregateRootModified(id1, 1)
    val receivedEventsFromPastCassandra = view.receiveN(3).map(_.asInstanceOf[EventEnvelope].event)
    view.expectMsg(MostOfTheEventsAlreadyReplayed)
    val receivedEventsFromPastKafka = view.receiveN(3).map(_.asInstanceOf[EventEnvelope].event)

    assert(receivedEventsFromPastCassandra.toSet == Set(event1_1, event2_1, event1_2))
    assert(receivedEventsFromPastCassandra.indexOf(event1_1) < receivedEventsFromPastCassandra.indexOf(event1_2))
    assert(receivedEventsFromPastKafka.toSet == Set(event1_1, event2_1, event1_2))
    assert(receivedEventsFromPastKafka.indexOf(event1_1) < receivedEventsFromPastKafka.indexOf(event1_2))

    val aggregate3 = system.actorOf(TestAggregateRootActor.props, id3.toString)

    aggregate1 ! ModifyTestAggregateRoot(id1, 11)
    aggregate2 ! ModifyTestAggregateRoot(id2, 12)
    aggregate3 ! CreateTestAggregateRoot(13, "ar3-1")
    aggregate3 ! ModifyTestAggregateRoot(id3, 14)

    receiveN(4) // wait for actual persist operations

    val event1_3 = TestAggregateRootModified(id1, 11)
    val event2_2 = TestAggregateRootModified(id2, 12)
    val event3_1 = TestAggregateRootCreated(id3, 13, "ar3-1")
    val event3_2 = TestAggregateRootModified(id3, 14)

    val currentEvents = view.receiveN(4).map(_.asInstanceOf[EventEnvelope].event)
    assert(currentEvents.toSet == Set(event1_3, event2_2, event3_1, event3_2))
    assert(currentEvents.indexOf(event3_1) < currentEvents.indexOf(event3_2))
  }
}
