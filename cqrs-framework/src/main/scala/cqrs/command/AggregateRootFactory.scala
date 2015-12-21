/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID

/**
 * Base class for write model aggregate factories.
 *
 * An aggregate factory defines how the aggregate instance is initialized. It receives CreateDomainCommands and returns
 * fully initialized aggregate instance. Due to the event sourcing, this is done in two steps: an event generation and
 * the event handling.
 *
 * Example of an aggregate factory implementation:
 *
 *  object AccountFactory extends AggregateRootFactory[Account]{
 *    override def handleCommand(id: UUID, eventBus: EventBus): CommandHandler = {
 *      case CreateAccountCommand => eventBus.publish(AccountCreated(id))
 *    }
 *
 *    override def applyEvent: EventHandler = {
 *      case AccountCreated(id) => Account(id, 0)
 *    }
 *  }
 */
trait AggregateRootFactory[AR <: AggregateRoot[AR]] {

  type CommandHandler = CreateDomainCommand => Unit

  type EventHandler = DomainEvent => AggregateRoot[AR]

  /**
   * Factory command handler accepts user commands for aggregate creation and optionally produces events using the
   * event bus passed as an argument.
   *
   * @param eventBus event bus reference enabling the factory to publish events
   */
  def handleCommand(id: UUID, eventBus: EventBus): CommandHandler

  /**
   * The event handler is responsible to build the initial state of the aggregate based on the received event. The event
   * may come from command handler or the event store if the aggregate state is replayed from scratch.
   */
  def applyEvent: EventHandler
}