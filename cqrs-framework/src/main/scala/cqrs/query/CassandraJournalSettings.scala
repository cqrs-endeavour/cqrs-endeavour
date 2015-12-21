/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.query

import java.net.InetSocketAddress

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy
import com.datastax.driver.core.{ Cluster, ConsistencyLevel }
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
 * Simple wrapper around [[Config]] object that makes access to Cassandra journal settings easier. 
 * 
 * Passed [[Config]] object should contain properties from cassandra-journal or cassandra-snapshot-store tree.
 */
private[cqrs] class CassandraJournalSettings(config: Config) extends Extension {
  val Keyspace: String = config.getString("keyspace")
  val Table: String = config.getString("table")

  val ReplicationFactor: Int = config.getInt("replication-factor")
  val ReadConsistency: ConsistencyLevel = ConsistencyLevel.valueOf(config.getString("read-consistency"))
  val WriteConsistency: ConsistencyLevel = ConsistencyLevel.valueOf(config.getString("write-consistency"))

  val ClusterBuilder: Cluster.Builder = Cluster.builder
    .addContactPointsWithPorts(config.getStringList("contact-points")
      .asScala
      .map(contactPoint => {
        val parts = contactPoint.split(":")

        if (parts.length != 2) {
          throw new IllegalArgumentException(s"Wrong Cassandra`s contact-point address: $contactPoint.")
        }

        new InetSocketAddress(parts(0), parts(1).toInt)
      }).asJavaCollection)

  if (config.hasPath("authentication")) {
    ClusterBuilder.withCredentials(
      config.getString("authentication.username"),
      config.getString("authentication.password"))
  }

  if (config.hasPath("local-datacenter")) {
    ClusterBuilder.withLoadBalancingPolicy(
      new DCAwareRoundRobinPolicy(config.getString("local-datacenter"))
    )
  }
  val ReplayDispatcherId: String = config.getString("replay-dispatcher")
  val MaxPartitionSize: Int = config.getInt("target-partition-size") // TODO: make persistent
  val MaxResultSize: Int = config.getInt("max-result-size")
}

private[cqrs] object CassandraJournalSettings extends ExtensionId[CassandraJournalSettings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): CassandraJournalSettings = {
    new CassandraJournalSettings(system.settings.config.getConfig("cassandra-journal"))
  }

  override def lookup(): CassandraJournalSettings.type = CassandraJournalSettings
}
