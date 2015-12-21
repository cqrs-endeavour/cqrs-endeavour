/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import scala.collection.mutable

/**
 * Simple filter that can be used to filter out duplicate events.
 *
 * Information about sequence numbers is recorded using [[markAsSeen]] method and can be persisted using 
 * [[updatePersistedSequenceNumbers]] by passing an implementation of [[EventDuplicatesFilterPersistService]].
 * After persisting it is best to mark those sequence numbers as persisted using [[commitSequenceNumbers]].
 * 
 * This class is not thread safe, it is assumed that only single thread or single actor will have access to it, e.g. single actor.
 * @param initialSequenceNumbers initial information about seen sequence numbers, e.g. can be fetched from data base using
 *                               implementation of [[EventDuplicatesFilterPersistService]]
 */
class EventDuplicatesFilter(initialSequenceNumbers: Seq[(String, Long)] = List()) {
  private val loggedSequenceNumbers = mutable.Map[String, Long]().withDefaultValue(-1L)

  private val committedSequenceNumbers = mutable.Map[String, Long](initialSequenceNumbers: _*).withDefaultValue(-1L)

  private def _wasNotSeen(envelope: EventEnvelope): Boolean = {
    val sequenceNumber = envelope.sequenceNr
    val lastCommittedSequenceNumber = committedSequenceNumbers(envelope.persistenceId)
    val lastLoggedSequenceNumber = loggedSequenceNumbers(envelope.persistenceId)

    lastCommittedSequenceNumber < sequenceNumber && lastLoggedSequenceNumber < sequenceNumber
  }

  /**
   * Marks given envelope as seen based on[[EventEnvelope.sequenceNr]]. 
   * 
   * @return true if given envelope was not seen, false otherwise
   */
  def markAsSeen(envelope: EventEnvelope): Boolean = {
    val wasNotSeen = _wasNotSeen(envelope)

    if (wasNotSeen) {
      loggedSequenceNumbers += envelope.persistenceId -> envelope.sequenceNr
    }

    wasNotSeen
  }

  /**
   * Persists sequence number information that was recorded since last [[commitSequenceNumbers]] method call.
   */
  def updatePersistedSequenceNumbers(service: EventDuplicatesFilterPersistService): Unit = {
    service.updateFilter(loggedSequenceNumbers.toSeq)
  }

  /**
   * Marks sequence numbers seen since last [[commitSequenceNumbers]] as persisted.
   */
  def commitSequenceNumbers(): Unit = {
    committedSequenceNumbers ++= loggedSequenceNumbers
    loggedSequenceNumbers.clear()
  }

  /**
   * Removes information about seen sequence numbers since last [[commitSequenceNumbers]]
   */
  def rollbackSequenceNumbers(): Unit = {
    loggedSequenceNumbers.clear()
  }

  override def toString: String = {
    s"committed = $committedSequenceNumbers, logged = $loggedSequenceNumbers"
  }
}
