/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID

/**
 * A base class for create (factory) commands which initialize aggregates.
 */
trait CreateDomainCommand extends DomainCommand {
  /*
    id field is present because it is workaround for possible bug in Akka cluster sharding.
    It seems that message that is returned from IdResolver is not passed to the ShardResolver
    https://groups.google.com/forum/#!topic/akka-user/vc6cRv3PXA8
  */
  private val _id = UUID.randomUUID()

  def id = _id
}