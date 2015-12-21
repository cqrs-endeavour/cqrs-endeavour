/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.testkit

import cqrs.command.{ AggregateRoot, DomainEvent, ModifyDomainCommand }

/**
 * Mutable wrapper around immutable [[AggregateRoot]] instance.
 * 
 * It can be used during testing of business logic to put [[AggregateRoot]] in specific state based
 * on passed events. When [[AggregateRoot]] is in desired state command cam be passed using
 * [[handleCommand]] and resulting event can be fetched from eventBus.
 */
class TestAggregateRootRef[T <: AggregateRoot[T]](aggregateRoot: T) {
  private var _currentAggregateRoot: AggregateRoot[T] = aggregateRoot
  
  val eventBus = new TestEventBus

  /**
   * Passes given command to wrapped [[AggregateRoot]] instance. Resulting event can be fetched from [[eventBus]].
   */
  def handleCommand(cmd: ModifyDomainCommand): Unit = {
    _currentAggregateRoot.handleCommand(eventBus)(cmd)
  }

  /**
   * Change [[AggregateRoot]] state based on [[DomainEvent]]
   * @return [[AggregateRoot]] instance with new event applied
   */
  def applyEvent(event: DomainEvent): AggregateRoot[T] = {
    _currentAggregateRoot = _currentAggregateRoot.applyEvent(event)

    _currentAggregateRoot
  }

  def applyEvents(events: Seq[DomainEvent]): T = {
    _currentAggregateRoot =
      events.foldLeft(_currentAggregateRoot)((ar: AggregateRoot[T], event) => ar.applyEvent(event))

    _currentAggregateRoot.asInstanceOf[T]
  }

  def currentAggregateRoot: AggregateRoot[T] = _currentAggregateRoot.asInstanceOf[T]
}
