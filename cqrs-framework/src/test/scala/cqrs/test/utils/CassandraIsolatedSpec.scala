/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.test.utils

import com.typesafe.config.Config
import cqrs.STActorSpec
import cqrs.test.utils.IntegrationTestConfigs._

object CassandraIsolatedSpec {
  def configuration: Config = {
    SystemProperties withFallback IsolatedCassandra withFallback DefaultAndOverrides
  }
}

class CassandraIsolatedSpec extends STActorSpec(CassandraIsolatedSpec.configuration)
