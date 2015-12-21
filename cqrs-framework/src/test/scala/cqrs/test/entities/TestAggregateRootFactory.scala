/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.test.entities

import java.util.UUID

import cqrs.command.{AggregateRootFactory, EventBus}
import cqrs.test.entities.TestAggregateRoot.{CreateTestAggregateRoot, TestAggregateRootCreated}

object TestAggregateRootFactory extends AggregateRootFactory[TestAggregateRoot] {
  override def handleCommand(id: UUID, eventBus: EventBus): TestAggregateRootFactory.CommandHandler = {
    case CreateTestAggregateRoot(a, b) => eventBus.publish(TestAggregateRootCreated(id, a, b))
  }

  override def applyEvent: TestAggregateRootFactory.EventHandler = {
    case event: TestAggregateRootCreated => TestAggregateRoot(event.id)
  }
}
