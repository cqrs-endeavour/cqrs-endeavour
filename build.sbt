/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._

lazy val cqrsFramework = project.in(file("cqrs-framework")).configs( MultiJvm)
  .settings(libraryDependencies ++= Dependencies.cqrsFramework)
  .settings(CommonSettings.commonSettings: _*)
  .settings(multiJvmSettings: _*)
  .settings(unmanagedSourceDirectories in MultiJvm <<= Seq(baseDirectory(_ / "src/test/scala")).join)
  .settings(parallelExecution in MultiJvm := false)
  .settings(parallelExecution in Test := false)
  .settings(scalatestOptions in MultiJvm ++= Seq("-u" , "target/test-reports"))
  .settings(bintrayReleaseOnPublish := false)
  .settings(licenses += ("MPL-2.0", url("https://www.mozilla.org/en-US/MPL/2.0/")))


lazy val root = (project in file("."))
  .aggregate(cqrsFramework)
