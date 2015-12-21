/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID
import annotation.unchecked._

object AggregateRoot {
  /**
   * A predefined state for marking an aggregate as deleted.
   */
  case class Removed(id: UUID) extends AggregateRoot[Nothing] {
    override def handleCommand(eventBus: EventBus): CommandHandler = ???
    override def handleQuery(query: AggregateQuery): Any = ???
    override def applyEvent: EventHandler = ???
  }
}

/**
 * Base class for write model aggregates.
 *
 * An aggregate is a transaction unit in the system. All commands addressed to a single aggregate are processed
 * sequentially. Aggregates need to define command processing, event processing and optionally query processing methods.
 *
 * Example of an aggregate implementation:
 *
 *  case class Account(id: UUID, balance: Long) extends AggregateRoot[Account] {
 *    override def handleCommand(eventBus: EventBus): CommandHandler = {
 *       case cmd@ChangeBalance(_, delta) => {
 *         val newBalance = delta + balance
 *         if (newBalance < 0) {
 *           throw new IllegalCommandException(cmd, s"Could not withdraw $delta when account $id has only $balance")
 *         } else {
 *           eventBus.publish(BalanceChanged(id, delta, newBalance))
 *         }
 *       }
 *     }
 *
 *     override def applyEvent: EventHandler = {
 *       case BalanceChanged(_, _, newBalance) => Account(id, newBalance)
 *     }
 *   }
 */
trait AggregateRoot[+T <: AggregateRoot[T]] {
  type CommandHandler = ModifyDomainCommand => Unit
  type EventHandler = DomainEvent => AggregateRoot[T @uncheckedVariance]

  /**
   * Command handler accepts user commands and optionally produces events using the event bus passed as an argument.
   * Usually it validates the command using the current state of the aggregate instance and if everything is ok the
   * state changing event is produced. The command handler do not changes the state on its own.
   *
   * @param eventBus event bus reference enabling the aggregate to publish events
   */
  def handleCommand(eventBus: EventBus): CommandHandler

  /**
   * Even though the CQRS principle advises to handle all the queries in the read model aggregates can also handle
   * queries. This feature may save you from creating a read model with the exactly the same projection shape as the
   * write model uses to process the commands.
   *
   * @param query A query coming from the user.
   * @return A query result that will be delivered to the user.
   */
  def handleQuery(query: AggregateQuery): Any

  /**
   * The event handler is responsible to build the state of the aggregate based on the received events. The aggregate's
   * state is designed to be immutable, hence the event handler returns a new copy of the aggregate. The separation of
   * the state mutation and command processing allows the framework to replay the state of the aggregate based on the
   * past events persisted in the event store without the need to persist the state in the database.
   */
  def applyEvent: EventHandler

  /**
   * A unique identifier of the aggregate. All events produces by the aggregate are tagged with the specified id.
   */
  def id: UUID

  /**
   * A predefined state for marking an aggregate as deleted.
   * Return it from the event handler when the remove event is processed.
   */
  protected def removed = AggregateRoot.Removed(id)
}
