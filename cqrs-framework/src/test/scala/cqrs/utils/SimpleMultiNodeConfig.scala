/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.utils

import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.ConfigFactory

abstract class SimpleMultiNodeConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")

  val akkaDebugConfig = ConfigFactory.parseResourcesAnySyntax("akka-debug")

  val akkaClusteringConfig = ConfigFactory.parseString(
    """
      akka.actor.provider = akka.cluster.ClusterActorRefProvider
      akka.remote.netty.tcp.port = 0
    """)
}