/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import akka.actor.ActorSystem
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.collection.JavaConversions._

private[cqrs] class CassandraJournalPersistenceIdsReader(implicit val system: ActorSystem) extends StrictLogging {
  private val config = CassandraJournalSettings(system)
  private def tableName = s"${config.Keyspace}.${config.Table}"
  private def selectMessagesQuery = s"SELECT DISTINCT persistence_id, partition_nr FROM $tableName"
  private def checkIfTableExistsQuery = s"SELECT columnfamily_name FROM system.schema_columnfamilies WHERE keyspace_name='${config.Keyspace}' and columnfamily_name='${config.Table}'"
  private lazy val cluster = config.ClusterBuilder.build
  private lazy val session = cluster.connect()

  def readAll(): Set[String] = {
    if (executeQuery(checkIfTableExistsQuery).isEmpty) {
      logger info "No table in Cassandra, returning empty set of ids."
      Set.empty
    }
    else {
      val result = executeQuery(selectMessagesQuery)
        .map(row => row.getString("persistence_id"))
        .toSet

      logger info s"Successfully read ${result.size} ids from cassandra."

      result
    }
  }

  private def executeQuery(query: String) = {
    val preparedQuery = session.prepare(query).setConsistencyLevel(config.ReadConsistency)
    session.execute(preparedQuery.bind()).iterator()
  }
}

