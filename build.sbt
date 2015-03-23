organization  := "feh.util"

name := "util"

version := "1.0.9-SNAPSHOT"

homepage := Some(url("https://github.com/fehu/util"))

licenses += "MIT" -> url("http://opensource.org/licenses/MIT")

// Build Settings

crossScalaVersions := Seq("2.11.5", "2.10.4")

scalaVersion := crossScalaVersions.value.head

libraryDependencies += "commons-io" % "commons-io" % "2.4" % "compile, runtime"

scalacOptions in (Compile, doc) ++= Seq("-diagrams")
