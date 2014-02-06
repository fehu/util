import sbt._
import Keys._
import org.sbtidea.SbtIdeaPlugin._

object Build extends sbt.Build {

  val ScalaVersion = "2.10.3"
  val Version = "1.0.2"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization  := "feh",
    version       := Version,
    scalaVersion  := ScalaVersion,
//    scalacOptions ++= Seq("-explaintypes"),
//    scalacOptions ++= Seq("-deprecation"),
    scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-diagrams-debug")
//     resolvers += Release.spray,
//     mainClass in Compile := Some("")
  )

  object Dependencies{
    object Apache{
      lazy val ioCommons = "commons-io" % "commons-io" % "2.4"
    }
  }

  import Dependencies._

  lazy val util = Project(
    id = "util",
    base = file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies += Apache.ioCommons
    )
  ).settings(ideaExcludeFolders := ".idea" :: ".idea_modules" :: Nil)

}