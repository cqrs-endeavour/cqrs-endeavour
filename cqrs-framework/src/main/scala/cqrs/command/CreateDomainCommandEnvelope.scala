/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.command

import java.util.UUID

/**
 * A wrapper create-type commands used to bypass an akka cluster sharding requirement for id specification in the
 * requests.
 *
 * Users do not provide identifiers in the factory commands and expect to have them generated by the server.
 * Cluster-sharded repositories intercept the create-type commands and enrich them with the generated identifier before
 * sending to a cluster-sharding aggregate discovery/delivery mechanism.
 */
case class CreateDomainCommandEnvelope(aggregateRootId: UUID, command: CreateDomainCommand)
