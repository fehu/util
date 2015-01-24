package feh.util.build

import feh.util.{RelativePath, Path}
import feh.util.file._
import scala.util.Try

trait ShellJarExecBuilder{
  def scriptFile(jar: Path): Try[File]
}

object ShellJarExecBuilder {
  val builder = sys.props.get("os.name") match{
    case Some(str) if str.toLowerCase.contains("linux") => new BashJarExecBuilder
  }
}

object ShellJarExecBuilderApp extends App {
  import ShellJarExecBuilder._
  println{
    builder.scriptFile(RelativePath(args.mkString(" "), File.separatorChar))
  }
}

class BashJarExecBuilder extends ShellJarExecBuilder{
  def scriptFile(jar: Path): Try[File] = {
    val filename = jar.reversed.head
    val scr = File.dropExt(filename) + ".sh"
    val f = File(jar.back / scr)
    val txt = scriptText(filename)
    Try{
      f.withOutputStream(_.write(txt.getBytes("UTF-8")))
      f
    }
  }

  def scriptText(jar: String) =
    s"""
      |#!/bin/sh
      |#
      |  java -jar $jar
      |
    """.stripMargin
}