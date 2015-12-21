/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.test.entities

import akka.actor.Props
import cqrs.repository.RepositoryProvider

object TestAggregateRootRepositoryProvider extends RepositoryProvider {
  override protected def localRepositoryProps: Props = LocalTestAggregateRootRepository.props

  override protected def aggregateRootProps: Props = TestAggregateRootActor.props
}