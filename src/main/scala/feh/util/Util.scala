package feh.util

import feh.util._

import scala.collection.generic.CanBuildFrom
import scala.collection.{TraversableLike, mutable}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import java.awt.Color
import scala.util.matching.Regex
import scala.language.{implicitConversions, higherKinds}

trait Util extends RandomWrappers{
  type I[T] = T => T
  type Y[A, B] = (A => B) => A => B
  type Y2[A1, A2, B] = ((A1, A2) => B) => (A1, A2) => B

  /**
   *  The fixed point combinator
   */
  def Y[A, B](rec: (A => B) => (A => B)): A => B = rec(Y(rec))(_: A)
  def Y2[A1, A2, B](rec: ((A1, A2) => B) => ((A1, A2) => B)): (A1, A2) => B = rec(Y2(rec))(_: A1, _: A2)

  def CY[A, B](rec: (A => B) => (A => B)): A => CYResult[A, B] = {
    val cache = mutable.HashMap.empty[A, B]
    def YY(f: (A => B) => (A => B)): A => B = {
      a => cache.getOrElse(a, {
        val b = rec(YY(rec))(a)
        cache += a -> b
        b
      })
    }
    YY(rec) andThen (res => CYResult(res, cache.toMap))
  }

  type Lifted[+T] = () => T

  implicit class Func1ApplyWrapper[A, R](f: A => R){
    def $(a: A) = f(a)
  }
  implicit class Func2ApplyWrapper[A, B, R](f: (A, B) => R){
    def $(a: A): B => R = f(a, _)
  }
  implicit class Func3ApplyWrapper[A, B, C, R](f: (A, B, C) => R){
    def $(a: A): B => C => R = f.curried(a)
  }
  implicit class Func4ApplyWrapper[A, B, C, D, R](f: (A, B, C, D) => R){
    def $(a: A): B => C => D => R = f.curried(a)
  }
  implicit class Func5ApplyWrapper[A, B, C, D, E, R](f: (A, B, C, D, E) => R){
    def $(a: A): B => C => D => E => R = f.curried(a)
  }

  implicit def curry[A, R](f: A => R): A => R = f // for uniformity
  implicit def curry[A, B, R](f: (A, B) => R): A => B => R = f.curried
  implicit def curry[A, B, C, R](f: (A, B, C) => R): A => B => C => R = f.curried
  implicit def curry[A, B, C, D, R](f: (A, B, C, D) => R): A => B => C => D => R = f.curried
  implicit def curry[A, B, C, D, E, R](f: (A, B, C, D, E) => R): A => B => C => D => E => R = f.curried

  implicit class PipeWrapper[T](t: => T){
    def pipe[R](f: T => R): R = f(t)
    def |>[R](f: T => R): R = f(t)
  }

  implicit class SeqPipeWrapper[C[_], T](t: => C[T]){
    def |>>[Q](opt: Option[T => Q], sel: C[T] => ((T => Q) => C[T])): C[T] = opt.map(sel(t)).getOrElse(t)

  }

  def lift[T](t: => T): Lifted[T] = () => t
  def liftUnit(t: => Any): Lifted[Unit] = () => t

  implicit class LiftWrapper[T](t: =>T){
    def lift = () => t
    def lifted = lift
    def liftUnit = () => t: Unit
  }

  object ImplicitLift{
    implicit def implicitLift[T](t: T): Lifted[T] = t.lifted
  }

  implicit class FilteringHelpingWrapper[+A, +Repr](tr: TraversableLike[A, Repr]){
    private def filter[B](f: A => B, v: B) = tr.filter(e => f(e) == v)

    def filterMax[B](f: A => B)(implicit cmp: Ordering[B]): Repr = filter(f, tr.maxBy(f) |> f)

    def filterMin[B](f: A => B)(implicit cmp: Ordering[B]): Repr = filter(f, tr.minBy(f) |> f)
  }

  def doUntil[T, R](initial: T, maxTry: Int)(f: T => Either[T, R]): Either[T, R] =
    Y[(T, Int), Either[T, R]](
      rec => {
        case (prev, c) =>
          f(prev) match {
            case Left(t) => if (c == maxTry)  Left(t)
                            else              rec(t -> (c+1))
            case right   => right
          }
      }
    )(initial -> 0)

  def elapsed[R](f: => R): (R, Duration) = {
    val time1 = System.nanoTime()
    val res = f
    val time2 = System.nanoTime()
    val dur = Duration(time2 - time1, NANOSECONDS)
    res -> dur
  }

  implicit class OptionWrapper[T](opt: Option[T]){
    def getOrThrow(thr: Throwable): T = opt.getOrElse(throw thr)
    def getOrThrow(msg: String): T = opt.getOrElse(sys.error(msg))
  }

  implicit class TripleBuilder[T1, T2](tuple: (T1, T2)){
    def -->[T3](t: T3) = (tuple._1, tuple._2, t)
  }

  implicit class MapZipperWrapper[A, B](map: Map[A, B]){
    def zipByKey[C](m2: Map[A, C]): Map[A, (B, C)] = {
      assert(map.keySet == m2.keySet, s"maps have different keys: ${m2.keySet &~ map.keySet }")
      map.map{
        case (k, v) => k -> (v, m2(k))
      }.toMap
    }
  }

  implicit class MutableMapZipperWrapper[A, B](map: mutable.Map[A, B]){
    def zipByKey[C](m2: mutable.Map[A, C]): Map[A, (B, C)] = zipByKey(m2.toMap)
    def zipByKey[C](m2: Map[A, C]): Map[A, (B, C)] = {
      assert(map.keySet == m2.keySet, s"maps have different keys: ${m2.keySet &~ map.keySet }")
      map.map{
        case (k, v) => k -> (v, m2(k))
      }
    }.toMap
  }

  implicit class SideEffectWrapper[T](t: T){
    def $$ (eff: T => Unit): T = {
      eff(t)
      t
    }
    def $$ (eff: => Unit): T = {
      eff
      t
    }
    def $$_if(cond: T => Boolean, caseTrue: T => Unit): T = {
      if(cond(t)) caseTrue(t)
      t
    }
  }

  implicit class LogWrapper[T](any: T){
    def log(msg: String): T = {
      println(msg)
      any
    }
    def log(msg: T => String): T = {
      println(msg(any))
      any
    }
  }

  implicit class TrySeqWrapper[T](seq: Seq[scala.util.Try[T]]){
    def flat = if(seq.isEmpty) Success(Nil) else ((Success(Seq()): scala.util.Try[Seq[T]]) /: seq){
      case (f@Failure(_), _) => f
      case (Success(acc), Success(next)) => Success(acc :+ next)
      case (_, Failure(fail)) => Failure(fail)
    }
  }

  implicit class ZipWrapper[C[t] <: TraversableOnce[t], A](c: C[A]) {
    def zipMap[B](f: A => B)(implicit builder: CanBuildFrom[C[A], (A, B), C[(A, B)]]): C[(A, B)] = {
      val b = builder()
      b ++= c.map(a => a -> f(a))
      b.result()
    }
  }

  implicit class TupleSeqWrapper[A, B](tr: Seq[(A, B)]){
    def mapVals[R](f: B => R) = tr.map{case (k, v) => k-> f(v)}
    def mapKeys[R](f: A => R) = tr.map{case (k, v) => f(k)-> v}
    def map2[R](f: (A, B) => R) = tr.map(f.tupled)
    def mapZipIn2[C, R](c: Seq[C])(f: (A, B, C) => R) = tr.zip(c).map{ case ((x, y), z) => f(x, y, z) }
  }

  implicit class TupleStreamWrapper[A, B](tr: Stream[(A, B)]){
    def mapVals[R](f: B => R) = tr.map{case (k, v) => k-> f(v)}
    def mapKeys[R](f: A => R) = tr.map{case (k, v) => f(k)-> v}
    def map2[R](f: (A, B) => R) = tr.map(f.tupled)
    def mapZipIn2[C, R](c: Seq[C])(f: (A, B, C) => R) = tr.zip(c).map{ case ((x, y), z) => f(x, y, z) }
  }

  implicit class MapWrapper[A, B](tr: Map[A, B]){
    def mapKeys[R](f: A => R) = tr.map{case (k, v) => f(k)-> v}
//    def zipMap[R](f: ((A, B)) => R) = tr.map{case p@(k, _) => k -> f(p)}
  }



  implicit class MutableMapWrapper[K, V](map: mutable.Map[K, V]){
    def <<=(key: K, upd: Option[V] => V): Unit  = map += key -> upd(map.get(key))
  }

  implicit class CastWrapper(a: Any){
    def cast[R] = a.asInstanceOf[R]
  }


  def tuple[A, T1, T2](t1: A => T1, t2: A => T2): A => (T1, T2) = a => (t1(a), t2(a))

  implicit class StringWrapper(str: String){
    def %(args: Any*) = str.format(args: _*)
    def apostrophised = "'" + str + "'"
    def quoted = "\"" + str + "\""
  }

  implicit class RegexMatcher(reg: Regex) {
    def matches(s: String) = reg.pattern.matcher(s).matches
  }

  object bool{
    def allOf[T](f: (T => Boolean)*): T => Boolean = t => f.forall(_(t))
    def oneOf[T](f: (T => Boolean)*): T => Boolean = t => f.exists(_(t))
  }


  implicit class InCaseWrapper[T](t: T){
    def inCase(test: T => Boolean) = if(test(t)) Some(t) else None
  }
  
  implicit class ImplicitApplyWrapper[T](t: => T){
    def implicitlyTo[R](implicit convert: T => R): R = convert(t)
  }

  implicit class ImplicitApplySeqWrapper[A](seq: Seq[A]){
    def implicitlyMapTo[B](implicit convert: A => B) = seq.map(convert)
  }

  implicit def toRunnableWrapper(func: () => Any): Runnable = new Runnable {
    def run() = func()
  }

  implicit final class EnsuringNot[A](private val self: A) {
    def ensuringNot(cond: Boolean): A = { assert(!cond); self }
    def ensuringNot(cond: Boolean, msg: => Any): A = { assert(!cond, msg); self }
    def ensuringNot(cond: A => Boolean): A = { assert(!cond(self)); self }
    def ensuringNot(cond: A => Boolean, msg: => Any): A = { assert(!cond(self), msg); self }
  }

  implicit class SeqDistinctByWrapper[A](seq: Seq[A]) {
    def distinctBy[B](f: A => B) = {
      var present = Set.empty[B]

      seq.filter{
        a =>
          val b = f(a)
          val res = !(present contains b)
          if (res) present += b
          res
      }
    }
  }


}

case object up extends Exception

case class CYResult[A, B](result: B, cache: Map[A, B])
