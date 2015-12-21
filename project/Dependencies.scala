/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import sbt.Keys._
import sbt._
import DependencyVersions._

object Dependencies {

  val typesafeConfig = "com.typesafe" % "config" % "1.2.1"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % AkkaVersion

  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion

  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % AkkaVersion

  val akkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion

  val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion

  val akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % AkkaVersion % "test"

  val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % AkkaVersion

  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % AkkaVersion

  val akkaPersistenceCassandra = "com.github.krasserm" %% "akka-persistence-cassandra" % AkkaPersistenceCassandraVersion

  val levelDB = "org.iq80.leveldb" % "leveldb" % LevelDBVersion

  val kafka = "org.apache.kafka" %% "kafka" % KafkaVersion exclude("javax.jms", "jms") exclude("com.sun.jdmk", "jmxtools") exclude("com.sun.jmx", "jmxri") exclude("org.slf4j", "slf4j-simple") exclude("org.slf4j", "slf4j-log4j12") exclude("log4j", "log4j")

  val cassandraDriver = "com.datastax.cassandra"  % "cassandra-driver-core" % CassandraVersion

  val logback = "ch.qos.logback" % "logback-classic" % LogbackVersion % Provided

  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging-slf4j" % ScalaLoggingVersion

  val log4jOverSlf4j = "org.slf4j" % "log4j-over-slf4j" % "1.7.12"  % Provided

  val scalaTest = "org.scalatest" %% "scalatest" % ScalaTestVersion % Test

  val akkaCore = Seq(akkaActor, akkaSlf4j, akkaTestkit)

  val akkaRemoteAndCluster = Seq(akkaRemote, akkaClusterSharding, akkaCluster, akkaMultiNodeTestkit, akkaClusterTools)

  val akkaPersistence = Seq("com.typesafe.akka" %% "akka-persistence" % AkkaVersion , akkaPersistenceCassandra, levelDB)

  val logging = Seq(logback, scalaLogging, log4jOverSlf4j)

  val commonDependencies = Seq(scalaTest)

  val cqrsFramework = commonDependencies ++ logging ++ akkaCore ++ akkaRemoteAndCluster ++ akkaPersistence ++
    Seq(kafka, cassandraDriver, akkaActor, akkaSlf4j)


  val additionalResolvers = Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/", "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven")
}
