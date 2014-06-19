package feh.util.sh

import feh.util.sh.exec.{ScalaVersion, Managed, LibInfoMeta, LibInfo}

object Libs {
  object feh{
    def util(version: String): LibInfo = LibInfo("feh.util", "util", version, Managed(dependsOnScala = true))
    def shUtil(version: String): LibInfo = LibInfo("feh.util", "shell-utils", version, Managed(dependsOnScala = true))

    lazy val util: LibInfo = util(_root_.feh.util.CurrentVersion.ver)
    lazy val shUtil: LibInfo = shUtil(_root_.feh.util.sh.CurrentVersion.ver)
  }

  object scala{
    def library(ver: ScalaVersion) = LibInfo("org.scala-lang", "scala-library", ver.version, Managed(dependsOnScala = false))
    def libAll(ver: ScalaVersion) = LibInfo("org.scala-lang", "scala-library-all", ver.version, Managed(dependsOnScala = false))

    def reflect(ver: ScalaVersion) = LibInfo("org.scala-lang", "scala-reflect", ver.version, Managed(dependsOnScala = false))
    def compiler(ver: ScalaVersion) = LibInfo("org.scala-lang", "scala-compiler", ver.version, Managed(dependsOnScala = false))
  }
}
