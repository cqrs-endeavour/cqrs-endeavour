/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

 import sbt.Keys._
import sbt._

object TestingConfiguration {

  def multiJvmTestFilter(name: String): Boolean = name contains "MultiJvm" // WARN: Hardcoded default value

  def unitTestFilter(name: String): Boolean = !multiJvmTestFilter(name)

  val unitTestConfiguration = testOptions in Test := Seq(Tests.Filter(unitTestFilter), Tests.Argument(TestFrameworks.Specs2, "junitxml", "console"))
}