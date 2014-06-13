import sbt._
import Keys._

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
  lazy val akka = "com.typesafe.akka" %% "akka-actor" % "2.3.3"

  object Apache{
    lazy val ioCommons = "commons-io" % "commons-io" % "2.4"
  }

  object scala{
    def compiler(version: String) = "org.scala-lang" % "scala-compiler" % version
    def swing(version: String) = "org.scala-lang" % "scala-swing" % version // todo: won't work for 2.11.1
    def reflectApi(version: String) = "org.scala-lang" % "scala-reflect" % version
  }

  lazy val treehugger = "com.eed3si9n" %% "treehugger" % "0.3.0"
  def scalaRefactoring(scalaVersion: String) = scalaVersion match{
    case v if v startsWith "2.10" => "org.scala-refactoring" %% "org.scala-refactoring" % "0.6.2-SNAPSHOT"
    case v if v startsWith "2.11" => "de.sciss" % "scalarefactoring_2.11" % "0.1.0"
  }

  object test{
    lazy val specs2 = "org.specs2" %% "specs2" % "2.3.12"
  }
}
