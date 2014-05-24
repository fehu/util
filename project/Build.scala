import sbt._
import Keys._
import org.sbtidea.SbtIdeaPlugin._

object Build extends sbt.Build {

  val ScalaVersions = Seq("2.10.3", "2.11.1")
  val MainVersion = "1.0.2"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    crossScalaVersions  := ScalaVersions,
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
      def compiler(version: String) = "org.scala-lang" % "scala-compiler" % version
      def swing(version: String) = "org.scala-lang" % "scala-swing" % version
      def reflectApi(version: String) = "org.scala-lang" % "scala-reflect" % version
    }

    lazy val treehugger = "com.eed3si9n" %% "treehugger" % "0.3.0"
    def scalaRefactoring(scalaVersion: String) = scalaVersion match{
      case v if v startsWith "2.10" => "org.scala-refactoring" %% "org.scala-refactoring" % "0.6.2-SNAPSHOT"
      case v if v startsWith "2.11" => "de.sciss" % "scalarefactoring_2.11" % "0.1.0"
    }
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
      libraryDependencies <++= scalaVersion {sv =>
        Seq(scala.compiler _, scalaRefactoring _).map(_(sv))
      }
    )
  ) dependsOn util

}