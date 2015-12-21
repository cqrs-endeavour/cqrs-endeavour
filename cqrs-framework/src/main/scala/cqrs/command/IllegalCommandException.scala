/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

/**
 * Thrown when the command cannot be processed by the aggregate.
 */
case class IllegalCommandException(command: DomainCommand, rejectCause: String)
  extends Exception(s"Cannot process ${command} - ${rejectCause}")