import sbt._
import Keys._
import org.sbtidea.SbtIdeaPlugin._

object Build extends sbt.Build {

  val ScalaVersion = "2.10.3"
  val MainVersion = "1.0.2"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    scalaVersion  := ScalaVersion,
    scalacOptions in (Compile, doc) ++= Seq("-diagrams")
//    scalacOptions ++= Seq("-explaintypes"),
//    scalacOptions ++= Seq("-deprecation"),
  )

  object Resolvers{
    object Release{
      val sonatype = "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
      val spray = "spray" at "http://repo.spray.io/"
    }

    object Snapshot{
      val sonatype = "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    }

  }

  object Dependencies{
    object Apache{
      lazy val ioCommons = "commons-io" % "commons-io" % "2.4"
    }

    object scala{
      lazy val compiler = "org.scala-lang" % "scala-compiler" % ScalaVersion
      lazy val swing = "org.scala-lang" % "scala-swing" % ScalaVersion
      lazy val reflectApi = "org.scala-lang" % "scala-reflect" % ScalaVersion
    }

    lazy val treehugger = "com.eed3si9n" %% "treehugger" % "0.3.0"
    lazy val scalaRefactoring = "org.scala-refactoring" %% "org.scala-refactoring" % "0.6.2-SNAPSHOT"
  }

  val publishLocalAll = TaskKey[Unit]("publish-all-local")

  lazy val rootSettings = buildSettings ++ Seq(
    publishLocalAll <<= state map {
      st =>
        Build.projects.foreach(p => Project.runTask(publishLocal in p, st))
    }
  )

  import Dependencies._
  import Resolvers._

  lazy val util = Project(
    id = "util",
    base = file("."),
    settings = rootSettings ++ Seq(
      organization  := "feh",
      version := MainVersion,
      libraryDependencies += Apache.ioCommons
    )
  ) .settings(ideaExcludeFolders := ".idea" :: ".idea_modules" :: Nil)

  lazy val scUtil = Project(
    id = "scala-compiler-utils",
    base = file("scutil"),
    settings = buildSettings ++ Seq(
      organization  := "feh.util",
      version := "0.1",
      resolvers += Snapshot.sonatype,
      libraryDependencies ++= Seq(scala.compiler, scalaRefactoring)
    )
  ) dependsOn util

}