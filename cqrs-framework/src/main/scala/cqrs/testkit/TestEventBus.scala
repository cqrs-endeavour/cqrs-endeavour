/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.testkit

import cqrs.command.{ DomainEvent, EventBus }

import scala.collection.mutable.ListBuffer

/**
 * Queue of published events.
 */
class TestEventBus extends EventBus {

  private val _publishedEvents = ListBuffer[DomainEvent]()

  override def publish(event: DomainEvent): Unit = {
    _publishedEvents.append(event)
  }

  def publishedEvents: List[DomainEvent] = _publishedEvents.toList

  def nextEvent: DomainEvent = {
    _publishedEvents.remove(0)
  }

  def nextEventOfClass[T]: T = {
    val event = nextEvent

    event.asInstanceOf[T]
  }
}
