/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.utils

import com.typesafe.config.ConfigFactory

object DistributedPubSubConfig extends SimpleMultiNodeConfig {
  val pubSubConfig = ConfigFactory.load("distributed-pub-sub-integration-tests")

  commonConfig(akkaDebugConfig.withFallback(akkaClusteringConfig).withFallback(pubSubConfig))
}