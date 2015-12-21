/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID

/**
 * Thrown when a Create-type command is received by an initialized aggregate.
 */
case class AggregateRootAlreadyExistsException(id: UUID) extends Exception
