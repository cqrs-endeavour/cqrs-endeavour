/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.utils

import java.util.UUID

import akka.remote.testconductor.RoleName
import com.typesafe.config.ConfigFactory
import cqrs.test.utils.IntegrationTestConfigs._

object ShardingClusterConfig extends SimpleMultiNodeConfig {

  val topic = s"test-topic-${UUID.randomUUID()}"

  val systemProperties = SystemProperties

  val persistenceConfig = IsolatedCassandra.withFallback(CassandraSmallPartition)

  val appConfig = ConfigFactory.parseString(
    s"""
      cqrs.repository-type = "sharded"
      cqrs.number-of-nodes = 10
      cqrs.pubsub = "simple"
      cqrs.pub-sub-topic = $topic
    """)

  val pubSubConfig = ConfigFactory.parseString("cqrs.pubsub = simple")

  def kafkaNodeName(role: RoleName) = ConfigFactory.parseString(s"cqrs.kafka-pub-sub.role-name=${role.name}")

  commonConfig(systemProperties.withFallback(akkaClusteringConfig).withFallback(appConfig).withFallback(persistenceConfig).withFallback(ConfigFactory.load()))

  nodeConfig(node1)(kafkaNodeName(node1))

  nodeConfig(node2)(kafkaNodeName(node1))
}
