/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import TestingConfiguration._
import sbt.Keys._

object CommonSettings {

  private val notPassablePropertyPrefixes = Vector("awt", "file", "java", "os", "path", "sun", "user")

  private def propertyTupleToJVMArgument(t: (String, String)): String = {
    s"-D${t._1}=${t._2}"
  }

  private def isPassableProperty(keyAndValue: (String, String)): Boolean = {
    val key = keyAndValue._1
    val keyElements = key.split('.')

    !notPassablePropertyPrefixes.contains(keyElements(0))
  }

  private def jvmPropertiesToPass: Seq[String] = {
    val systemProperties = collection.JavaConversions.propertiesAsScalaMap(System.getProperties)

    systemProperties.filter(isPassableProperty).map(propertyTupleToJVMArgument).toSeq
  }

  val commonSettings = Seq(
    organization := "cqrs-framework",
    javaOptions ++= jvmPropertiesToPass,
    javaOptions ++= Seq("-XX:MaxPermSize=128M", "-Xms512M", "-Xmx1536M", "-server"),
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xlint",
      //"-Xfatal-warnings",
      "-Ywarn-dead-code",
      "-language:_",
      "-encoding", "UTF-8"
    ),
    fork := true,
    unitTestConfiguration,
    resolvers ++= Dependencies.additionalResolvers
  )  ++ net.virtualvoid.sbt.graph.Plugin.graphSettings
}
