import feh.util.TestReportsCopy
import sbt._
import Keys._
import Dependencies._
import Resolvers._

object Build extends sbt.Build {

  val ScalaVersions = Seq("2.11.5", "2.10.3")
  val ScalaVersion = ScalaVersions.head

  val MainVersion = "1.0.6"

  // // // //  settings presets  // // // //

  val buildSettings = Defaults.coreDefaultSettings ++ Defaults.defaultConfigs ++ Seq (
    organization  := "feh.util",
    scalaVersion := ScalaVersion,
    crossScalaVersions  := ScalaVersions,
    scalacOptions in (Compile, doc) ++= Seq("-diagrams"),
    isSnapshot := version.value.toLowerCase.endsWith("snapshot")
  )


  lazy val testSettings = TestReportsCopy.settings ++ Seq(
    libraryDependencies += Dependencies.test.specs2,
    TestReportsCopy.copyTestReportsDir <<= baseDirectory(base => Some(base / "test-reports")),
    TestReportsCopy.autoAddReportsToGit := true
  )

  object Licenses{
    lazy val MIT = "MIT" -> url("http://opensource.org/licenses/MIT")
  }

  lazy val licenceSettings = Seq(
    homepage := Some(url("https://github.com/fehu/util")),
    licenses += Licenses.MIT
  )

  // // // // // //  projects  // // // // // //

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = buildSettings ++ testSettings ++ Seq(
      version := MainVersion,
      publishArtifact := false
    )
  ).aggregate(util, compiler, shell)

  lazy val util = Project(
    id = "util",
    base = file("util"),
    settings = buildSettings ++ licenceSettings ++ Seq(
      version := MainVersion,
      libraryDependencies += Apache.ioCommons
    )
  )

  lazy val compiler = Project(
    id = "scala-compiler-utils",
    base = file("compiler"),
    settings = buildSettings ++ licenceSettings ++ Seq(
      version := "0.2-SNAPSHOT",
      resolvers += Snapshot.sonatype,
      libraryDependencies <++= scalaVersion {sv =>
        Seq(scala.compiler _, scalaRefactoring _).map(_(sv))
      }
    )
  ) dependsOn util

  lazy val shell = Project(
    id = "shell-utils",
    base = file("shell"),
    settings = buildSettings ++ testSettings ++ licenceSettings ++ Seq(
      version := "0.2-SNAPSHOT",
      libraryDependencies ++= Seq(Apache.ioCommons, akka)
    )
  ) dependsOn util

}
