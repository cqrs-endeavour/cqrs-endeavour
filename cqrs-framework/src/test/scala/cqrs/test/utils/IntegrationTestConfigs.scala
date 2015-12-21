/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.test.utils

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}

object IntegrationTestConfigs {
  def SystemProperties: Config = ConfigFactory.defaultOverrides()

  def IsolatedCassandra: Config = {
    val tableName = "test" + UUID.randomUUID().toString.replace("-", "")

    ConfigFactory.parseString("cassandra-journal.table = \"" + tableName + "\"")
      .withFallback(ConfigFactory.parseString("akka.test.single-expect-default = 10s"))
      .withFallback(ConfigFactory.parseResourcesAnySyntax("persistence-integration-tests"))
  }

  def IsolatedKafka: Config = {
    ConfigFactory.parseString("cqrs.pub-sub-topic = \"" + UUID.randomUUID().toString + "\"")
      .withFallback(ConfigFactory.parseResourcesAnySyntax("kafka-integration-tests"))
  }

  def CassandraSmallPartition: Config = {
    ConfigFactory.parseString("cassandra-journal.target-partition-size = 1")
      .withFallback(ConfigFactory.parseString("cassandra-journal.config-table=configsamllpartition"))
  }

  def DefaultAndOverrides: Config = ConfigFactory.load()
}
