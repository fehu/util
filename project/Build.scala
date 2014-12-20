import sbt._
import Keys._
import Dependencies._
import Resolvers._

object Build extends sbt.Build {

  val ScalaVersions = Seq("2.11.2", "2.10.3")
  val ScalaVersion = ScalaVersions.head

  val MainVersion = "1.0.5"

  // // // //  settings presets  // // // //

  val buildSettings = Defaults.coreDefaultSettings ++ Defaults.defaultConfigs ++ Seq (
    organization  := "feh.util",
    scalaVersion := ScalaVersion,
    crossScalaVersions  := ScalaVersions,
    scalacOptions in (Compile, doc) ++= Seq("-diagrams"),
    isSnapshot := version.value.toLowerCase.endsWith("snapshot")
  )


  lazy val testSettings = TestSettings.get ++ Seq(
    TestSettings.copyTestReportsDir <<= baseDirectory(base => Some(base / "test-reports")),
    TestSettings.autoAddReportsToGit := true
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
    settings = buildSettings ++ testSettings ++ /*PublishingSettings.get ++*/ Seq(
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
      version := "0.1",
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
      version := "0.1",
      libraryDependencies ++= Seq(Apache.ioCommons, akka)
    )
  ) dependsOn util

}
