/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import java.util.UUID

import cqrs.test.entities.TestAggregateRoot.TestAggregateRootCreated
import org.scalatest.FlatSpec

class InMemoryEventDuplicatesFilterPersistService extends EventDuplicatesFilterPersistService {
  private var _lastSequenceNumbers: Seq[(String, Long)] = List()

  override def fetchFilter: EventDuplicatesFilter = ???

  override def updateFilter(updatedSequenceNumbers: Seq[(String, Long)]): Unit = {
    _lastSequenceNumbers = updatedSequenceNumbers
  }

  def lastSequenceNumbers: Seq[(String, Long)] = {
    _lastSequenceNumbers
  }
}

trait EventDuplicatesFilterSpecScope {
  val service = new InMemoryEventDuplicatesFilterPersistService
  val filter = new EventDuplicatesFilter
  val event = TestAggregateRootCreated(UUID.randomUUID(), 2, "some_event")
}

class EventDuplicatesFilterSpec extends FlatSpec {
  "EventDuplicatesFilter" should "allow consecutive events for one aggregate" in new EventDuplicatesFilterSpecScope {
    assert(filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 4, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 5, event)))
  }

  it should "filter events with the same sequence number" in new EventDuplicatesFilterSpecScope {
    assert(filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
    assert(!filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 3, event)))
  }

  it should "filter events with smaller sequence number" in new EventDuplicatesFilterSpecScope {
    assert(filter.markAsSeen(EventEnvelope("ar-1", 3, event)))
    assert(!filter.markAsSeen(EventEnvelope("ar-1", 1, event)))
    assert(!filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 4, event)))
  }

  it should "allow consecutive events within different aggregagtes" in new EventDuplicatesFilterSpecScope {
    assert(filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-2", 2, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 3, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-2", 3, event)))
  }

  it should "filter events with the same sequence and aggregate id" in new EventDuplicatesFilterSpecScope {
    assert(filter.markAsSeen(EventEnvelope("ar-1", 1, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-2", 2, event)))
    assert(!filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
    assert(!filter.markAsSeen(EventEnvelope("ar-2", 2, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-2", 3, event)))
  }

  it should "rollback seen but not committed events" in new EventDuplicatesFilterSpecScope {
    // given
    assert(filter.markAsSeen(EventEnvelope("ar-1", 1, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 2, event)))

    // when
    filter.rollbackSequenceNumbers()

    // then
    assert(filter.markAsSeen(EventEnvelope("ar-1", 1, event)))
    assert(filter.markAsSeen(EventEnvelope("ar-1", 2, event)))
  }

  it should "use service to persist not committed sequence numbers" in new EventDuplicatesFilterSpecScope {
    // given
    val firstEnvelope = EventEnvelope("ar-1", 1, event)
    val secondEnvelope = EventEnvelope("ar-2", 2, event)
    assert(filter.markAsSeen(firstEnvelope))
    assert(filter.markAsSeen(secondEnvelope))

    // when
    filter.updatePersistedSequenceNumbers(service)

    // then
    val expectedSequenceNumbers = Seq("ar-1" -> 1, "ar-2" -> 2)
    assert(service.lastSequenceNumbers == expectedSequenceNumbers)
  }
}
