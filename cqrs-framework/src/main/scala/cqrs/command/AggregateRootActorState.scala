/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID

import cqrs.command.AggregateRoot.Removed

/**
 * A wrapper for the aggregate state. Takes over the handling of (un)initialized states from the user. Introduces
 * AggregateRoot factory and two types of commands to make it work: CreateDomainCommand and ModifyDomainCommand.
 *
 * @param id An identifier of the aggregate instance.
 * @param bus The event bus for publishing new events.
 * @param factory Aggregate factory instance handling CreateDomainCommands.
 */
class AggregateRootActorState[AR <: AggregateRoot[AR]](id: UUID, bus: EventBus, factory: AggregateRootFactory[AR]) {
  private var _aggregateRoot: Option[AggregateRoot[AR]] = None

  def initialized: Boolean = {
    _aggregateRoot.isDefined
  }

  def aggregateRoot: AggregateRoot[AR] = {
    if (initialized) {
      _aggregateRoot.get
    }
    else {
      throw AggregateRootNotInitializedException(id)
    }
  }

  def handleCommand: PartialFunction[DomainCommand, Unit] = {
    case cmd: CreateDomainCommand if !initialized => {
      factory.handleCommand(id, bus)(cmd)
    }

    case cmd: CreateDomainCommand if initialized => {
      throw new AggregateRootAlreadyExistsException(id)
    }

    case cmd: ModifyDomainCommand if !initialized => {
      throw new AggregateRootNotInitializedException(id)
    }

    case cmd: ModifyDomainCommand if initialized => {
      if (cmd.aggregateRootId != id) {
        throw new IllegalCommandException(cmd, s"$cmd was addressed to ${cmd.aggregateRootId} not $id")
      }
      else {
        aggregateRoot.handleCommand(bus)(cmd)
      }
    }

    case cmd: DomainCommand => {
      throw new IllegalCommandException(cmd, s"Can't handle $cmd in initialized = $initialized state")
    }
  }

  def handleQuery(query: AggregateQuery): Any = {
    if (!initialized) {
      throw new AggregateRootNotInitializedException(id)
    }

    aggregateRoot.handleQuery(query)
  }

  def updateAggregateRoot(event: DomainEvent) = {
    val nextState = if (initialized) aggregateRoot.applyEvent(event) else factory.applyEvent(event)
    handleNewAggregateRootInstance(nextState)
  }

  private def handleNewAggregateRootInstance: PartialFunction[AggregateRoot[AR], Unit] = {
    case Removed(_) => {
      _aggregateRoot = None
    }

    case ar: AggregateRoot[AR] => {
      _aggregateRoot = Option(ar)
    }
  }

  def acceptSnapshotOffer(snapshot: AggregateRoot[AR]) = {
    _aggregateRoot = Option(snapshot)
  }
}