/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID

/**
 * Thrown when a non-create-type command is received by an non-initialized aggregate.
 */
case class AggregateRootNotInitializedException(id: UUID) extends Exception(id.toString)