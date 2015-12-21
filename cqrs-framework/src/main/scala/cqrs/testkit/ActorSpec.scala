/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs.testkit

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem, PoisonPill, Terminated }
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import com.typesafe.config.{ Config, ConfigFactory }
import cqrs.testkit.ActorSpec._

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

object ActorSpec {
  def createActorSystemName = s"TestActorSystem-${UUID.randomUUID().toString.replaceAll("-", "")}"
}

/**
 * This enhances standard akka [[TestKit]] with useful methods for actor testing.
 * 
 * Actor system name is random so it is possible to spawn more than one actor system during testing using this class.
 * @param config configuration of actor system
 */
abstract class ActorSpec(config: Config) extends TestKit(ActorSystem(createActorSystemName, config))
    with ImplicitSender {

  def this() = this(ConfigFactory.load())

  /**
   * Request-response cycle wrapper. 
   * 
   * Passed message is sent to an actor, after that this method blocks until next message is received. 
   * If this message is not of responseType class or timeout passes exception is thrown.
   */
  def waitForMessageProcessing[C](actor: ActorRef, message: Any, responseType: Class[C]): Unit = {
    actor ! message
    expectMsgClass(responseType)
  }

  /**
   * Stops given actor and waits for [[Terminated]] message.
   */
  def stopActor(actor: ActorRef) = {
    watch(actor)
    actor ! PoisonPill
    expectMsgType[Terminated]
  }

  /**
   * Gathers X message from passed probe, this method returns if duplicated message is detected.
   */
  def receiveUniqueMessages[T](probe: TestProbe)(implicit t: ClassTag[T]): Seq[T] = {
    val messages = ListBuffer[T]()

    probe.receiveWhile() {
      case msg: T => {
        if (!messages.contains(msg)) {
          messages += msg
        }
        msg
      }
    }

    messages.toSeq
  }
}
