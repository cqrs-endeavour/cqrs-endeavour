/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package cqrs

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension

class AkkaSerializer(system: ActorSystem) {
  private val serializer = SerializationExtension(system)

  def serialize(obj: AnyRef): Array[Byte] = {
    serializer.serialize(obj).get
  }

  def deserialize[T](bytes: Array[Byte], clazz: Class[T]): T = {
    serializer.deserialize(bytes, clazz).get
  }
}
