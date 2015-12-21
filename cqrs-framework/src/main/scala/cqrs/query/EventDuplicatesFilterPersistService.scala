/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

/**
 * Implementations of this trait should be able to persist information about new sequence numbers and 
 * reconstruct [[EventDuplicatesFilter]] based on information in storage engine.
 */
trait EventDuplicatesFilterPersistService {
  def fetchFilter(): EventDuplicatesFilter

  def updateFilter(updatedSequenceNumbers: Seq[(String, Long)])
}
