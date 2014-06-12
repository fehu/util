package feh.util.build

import feh.util.FileUtils
import scala.util.Try
import java.io.File

trait ShellJarExecBuilder extends FileUtils{
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
    builder.scriptFile(builder.RelativePath.raw(args.toSeq))
  }
}

class BashJarExecBuilder extends ShellJarExecBuilder{
  def scriptFile(jar: Path): Try[File] = {
    val filename = jar.reversed.head
    val scr = dropExt(filename) + ".sh"
    val f = file(jar.back / scr)
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