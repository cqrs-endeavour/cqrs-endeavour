/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs

import akka.actor.ActorRef
import com.typesafe.config.Config
import cqrs.test.entities.TestPersistentActor.RemoveMessages
import cqrs.testkit.ActorSpec
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class STActorSpec(config: Config) extends ActorSpec(config) with FlatSpecLike with BeforeAndAfterAll {

  private val shutdownDuration: FiniteDuration = 10.seconds

  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), shutdownDuration)
    super.afterAll()
  }

  def stopAndCleanPersistentActors(refs: ActorRef*): Unit = {
    refs.foreach { ref =>
      watch(ref)
      ref ! RemoveMessages
      expectTerminated(ref)
    }
  }
}
