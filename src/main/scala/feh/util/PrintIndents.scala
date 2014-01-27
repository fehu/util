package feh.util

import feh.util.AbstractScopedState.IgnoreUpdate

trait PrintIndents {
  class Param protected[PrintIndents] (val b: StringBuilder,
                                       protected[PrintIndents] val depthState: ScopedState[Int] with IgnoreUpdate[Int],
                                       protected[PrintIndents] var indent: Int)
  {
    def withDepth[R](d: Int => Int) = depthState.doWith[R](d(depthState.get)) _
    def mkString = b.mkString
    def depth = depthState.get
  }

  def newBuilder(indent: Int) = new Param(StringBuilder.newBuilder, new ScopedState(0) with IgnoreUpdate[Int], indent)
  
  /** print line with indent
   */
  def printlni(str: String)(implicit p: Param){
    p.b ++= " " * (p.indent * p.depthState.get) + str + "\n"
  }

  def nextDepth[R](f: => R)(implicit p: Param) = p.withDepth(1+)(f)

  protected def ignoreDepthIncrease[R](n: Int)(f: => R)(implicit p: Param) = p.depthState.ignoring[R](n)(f)

  def printY[A](next: (A => Unit) => (A => Unit))(apply: A)(implicit p: Param) {
    def inner(f: (Param => A => Unit) => (Param => A => Unit)): (Param => A => Unit) = f(inner(f))(_: Param)

    ignoreDepthIncrease(1){
      inner(
        rec => param => a => nextDepth(next(rec(param))(a))
      )(p)(apply)
    }
  
    }
    
}

object PrintIndents extends PrintIndents
