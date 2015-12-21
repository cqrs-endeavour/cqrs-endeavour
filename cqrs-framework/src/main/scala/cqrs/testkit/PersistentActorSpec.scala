/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.testkit

import java.util.UUID

import akka.pattern.ask
import akka.testkit.DefaultTimeout
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import cqrs.command.DomainEvent
import cqrs.testkit.EventGatheringActor.{GetEvents, ReplayedEvents}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Class that enhances [[ActorSpec]] with methods useful for aggregate root testing.
 * 
 * @param config configuration of actor system
 */
abstract class PersistentActorSpec(config: Config) extends ActorSpec(config) with DefaultTimeout {

  def this(configName: String) = {
    this(ConfigFactory.load(configName))
  }

  def gatherEventsFor(id: UUID): Vector[DomainEvent] = gatherEventsFor(id.toString)

  /**
   * Gather all events from journal for aggregate root with given id. Waits
   */
  def gatherEventsFor(id: String)(implicit waitTime: FiniteDuration = 5.seconds): Vector[DomainEvent] = {
    val gatherActor = system.actorOf(EventGatheringActor(id))
    implicit val timeout: Timeout = waitTime

    val replayedEvents = Await.result(gatherActor ? GetEvents, waitTime).asInstanceOf[ReplayedEvents]
    replayedEvents.events
  }
}
