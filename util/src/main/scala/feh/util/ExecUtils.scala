package feh.util

import java.lang.ProcessBuilder.Redirect
import java.io.File
import java.text.NumberFormat

object ExecUtils extends ExecUtils
trait ExecUtils extends Debugging{

  var debug = false 
  def debugMessagePrefix = "ExecUtils"

  trait ArgsImplicits{
    def args(list: String*) = list


    implicit def Long = (_: Long).toString
    implicit def Int  = (_: Int).toString

    object Boolean{
      implicit def asString = (_: Boolean).toString
      implicit def asNum = (b: Boolean) => if (b) "1" else "0"
    }

    def Double(precision: Int) = (d: Double) => {
      val format = NumberFormat.getInstance()
      format.setMaximumFractionDigits(precision)
      format.format(d)
    }
  }

  object ArgsImplicits extends ArgsImplicits

  private val redirectState = new ScopedState[Boolean](false)
  def redirecting_? = redirectState.get
  def redirectingStreams[R](f: => R): R = redirectState.doWith(true)(f)

  private val setExecState = new ScopedState[Boolean](false)
  def setExec_? = setExecState.get
  // todo: test what happens with permission after
  def inExecutableMode[R](f: => R): R = setExecState.doWith(true)(f)

  def exec(args: Seq[String]): Process = {
    debugLog(s"## executing command: ${args.mkString(" ")}")
    val builder = new ProcessBuilder(args: _*)
    
    if(redirecting_?) {
      builder.redirectOutput(Redirect.PIPE)
      builder.redirectError(Redirect.PIPE)
    }
    if(setExec_?) new File(args.head) |> {
      f =>
        assert(f.exists(), s"$f doesn't exist")
        assert(!f.isDirectory, s"$f is a directory")
        
        assert(f.setExecutable(true), s"failed to set execution permission for file $f")
    }
    builder.start()
  }

  def exec[S <% String](cmd: String, args: S*): Process = exec(cmd +: args.implicitlyMapTo[String])

  def sbt(cmd: String, args: String*) = exec("sbt" +: cmd +: args :+ "-no-colors")

}

