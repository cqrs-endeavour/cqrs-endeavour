/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.utils

import akka.cluster.Cluster
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender

import scala.concurrent.duration.FiniteDuration

abstract class ClusterSpec(config: SimpleMultiNodeConfig) extends MultiNodeSpec(config) with STMultiNodeSpec with ImplicitSender {
  def initialParticipants = roles.size

  def join(startOn: RoleName, joinTo: RoleName) {
    runOn(startOn) {
      Cluster(system) join node(joinTo).address
    }
    enterBarrier(startOn.name + "-joined")
  }

  def joinCluster() {
    join(startOn = config.node1, joinTo = config.node1)
    join(startOn = config.node2, joinTo = config.node1)
    enterBarrier("cluster-created")
  }

  def waitDuration(duration: FiniteDuration, purpose: String) = {
    enterBarrier(s"starting to wait $duration for $purpose")
    Thread.sleep(duration.toMillis)
    enterBarrier(s"finished waiting $duration for $purpose")
  }
}