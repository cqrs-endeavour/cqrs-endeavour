/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.pubsub

import akka.actor._
import com.typesafe.scalalogging.slf4j.StrictLogging

private[cqrs] object ReplayablePubSubExtension extends ExtensionId[ReplayablePubSubExtension] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): ReplayablePubSubExtension = {
    new ReplayablePubSubExtension(system)
  }

  override def lookup(): ReplayablePubSubExtension.type = ReplayablePubSubExtension
}

/**
 * Akka extension for [[ReplayablePubSub]] facilitating the usage in the Akka environment.
 * Extension is the Akka idiomatic configuration pattern.
 */
private[cqrs] class ReplayablePubSubExtension(system: ExtendedActorSystem) extends Extension with StrictLogging {

  private def providePubSub: ActorRef = {
    val config = system.settings.config.getConfig("cqrs")
    val pubSubName = config.getString("pubsub")

    logger.info(s"Creating ReplayablePubSubExtension, pubSubName = ${pubSubName}")

    pubSubName match {
      case "distributed-pub-sub" => system.actorOf(Props[DistributedPubSub], pubSubName)
      case "simple" => system.actorOf(Props[SimplePubSub], pubSubName)
      case "kafka" => system.actorOf(Props[KafkaPubSub], pubSubName)
      case name => throw new IllegalArgumentException(s"Invalid name of pub sub in config: $name")
    }
  }

  val pubSub = providePubSub
}