import sbt._
import Keys._

object Build extends sbt.Build {

  val Version = "1.0.7-SNAPSHOT"

  // // // //  settings presets  // // // //

  val buildSettings = Defaults.coreDefaultSettings ++ Defaults.defaultConfigs ++ Seq (
    organization  := "feh.util",
    crossScalaVersions := Seq("2.11.5", "2.10.3"),
    scalacOptions in (Compile, doc) ++= Seq("-diagrams")
  )

  object Licenses{
    lazy val MIT = "MIT" -> url("http://opensource.org/licenses/MIT")
  }

  lazy val licenceSettings = Seq(
    homepage := Some(url("https://github.com/fehu/util")),
    licenses += Licenses.MIT
  )

  // // // // // //  projects  // // // // // //

  lazy val util = Project(
    id = "util",
    base = file("util"),
    settings = buildSettings ++ licenceSettings ++ Seq(
      version := Version,
      libraryDependencies += "commons-io" % "commons-io" % "2.4" % "compile, runtime"
    )
  )

}
