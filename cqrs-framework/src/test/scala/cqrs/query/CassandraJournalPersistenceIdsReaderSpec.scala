/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import java.util.UUID

import cqrs.test.entities.TestPersistentActor
import cqrs.test.entities.TestPersistentActor._
import cqrs.test.utils.{CassandraIsolatedSpec, CassandraSmallPartitionSpec}

class CassandraJournalPersistenceIdsReaderSpec extends CassandraIsolatedSpec {
  "CassandraJournalReader" should "return empty list when there is no table yet" in {
    val reader = new CassandraJournalPersistenceIdsReader
    val persistentIds = reader.readAll()

    assert(persistentIds.toList.isEmpty)
  }

  it should "retrieve the persistence ids" in {
    val id1 = s"id1-${UUID.randomUUID()}"
    val id2 = s"id2-${UUID.randomUUID()}"
    val actor1 = system.actorOf(TestPersistentActor.props(id1))
    val actor2 = system.actorOf(TestPersistentActor.props(id2))

    actor1 ! TestCommand(id1, 1, "first")
    actor2 ! TestCommand(id2, 5, "second")
    actor1 ! TestCommand(id1, 20, "third")

    // wait for confirmation that it was persisted
    expectMsg(PersistedAck)
    expectMsg(PersistedAck)
    expectMsg(PersistedAck)

    val reader = new CassandraJournalPersistenceIdsReader
    val persistentIds = reader.readAll()

    assert(persistentIds.toSet == Set(id1, id2))
  }
}

class CassandraJournalPersistenceIdsReaderDifferentPartitionsSpec extends CassandraSmallPartitionSpec {

  "CassandraJournalReader" should "retrieve the persistence ids" in {
    val id1 = s"id1-${UUID.randomUUID()}"
    val id2 = s"id2-${UUID.randomUUID()}"
    val actor1 = system.actorOf(TestPersistentActor.props(id1))
    val actor2 = system.actorOf(TestPersistentActor.props(id2))

    actor1 ! TestCommand(id1, 2, "first")
    actor2 ! TestCommand(id2, 5, "second")
    actor1 ! TestCommand(id1, 20, "third")

    // wait for confirmation that it was persisted
    expectMsg(PersistedAck)
    expectMsg(PersistedAck)
    expectMsg(PersistedAck)

    val reader = new CassandraJournalPersistenceIdsReader
    val persistentIds = reader.readAll()

    assert(persistentIds.toSet == Set(id1, id2))
    stopAndCleanPersistentActors(actor1, actor2)
  }
}
