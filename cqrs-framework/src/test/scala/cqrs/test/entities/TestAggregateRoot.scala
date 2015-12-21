/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.test.entities

import java.util.UUID

import cqrs.command._

object TestAggregateRoot {

  sealed trait ModifyTestAggregateRootCommand extends ModifyDomainCommand {
    def id: UUID

    override def aggregateRootId: UUID = id
  }

  sealed trait CreateTestAggregateRootCommand extends CreateDomainCommand

  @SerialVersionUID(1L)
  case class CreateTestAggregateRoot(a: Int, b: String) extends CreateTestAggregateRootCommand

  @SerialVersionUID(1L)
  case class ModifyTestAggregateRoot(id: UUID, data: Int) extends ModifyTestAggregateRootCommand

  @SerialVersionUID(1L)
  case class TestAggregateRootCreated(id: UUID, a: Int, b: String) extends DomainEvent

  @SerialVersionUID(1L)
  case class TestAggregateRootModified(id: UUID, data: Int) extends DomainEvent

}

case class TestAggregateRoot(id: UUID) extends AggregateRoot[TestAggregateRoot] {

  import TestAggregateRoot._

  override def handleCommand(eventBus: EventBus): CommandHandler = {
    case ModifyTestAggregateRoot(id, data) => eventBus.publish(TestAggregateRootModified(id, data))
  }

  override def applyEvent: EventHandler = {
    case cmd: TestAggregateRootModified => this
  }

  override def handleQuery(query: AggregateQuery): Any = ???
}
