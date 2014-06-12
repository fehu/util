package feh.util

import java.lang.ProcessBuilder.Redirect

object ExecUtils extends ExecUtils
trait ExecUtils {

  private def exec(args: Seq[String], redirectStreams: Boolean): Process = {
    println(s"## executing command: ${args.mkString(" ")}")
    val builder = new ProcessBuilder(args: _*)
    builder.redirectErrorStream(true)
    if(redirectStreams) builder.redirectOutput(Redirect.PIPE)
    builder.start()
  }

  private val redirectState = new ScopedState[Boolean](false)
  def redirecting = redirectState.get

  def redirectingStreams[R](f: => R): R = redirectState.doWith(true)(f)

  def exec(cmd: String, args: String*): Process = exec(cmd +: args, redirectState.get)

  def sbt(cmd: String, args: String*) = exec("sbt" +: cmd +: args :+ "-no-colors", redirectState.get)

}
