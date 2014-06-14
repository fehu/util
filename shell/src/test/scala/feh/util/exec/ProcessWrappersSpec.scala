package feh.util.exec

import org.specs2._
import scala.concurrent.duration.FiniteDuration
import feh.util.ExecUtils
import akka.actor.ActorSystem
import scala.concurrent.{Future, Await}

object ProcessWrappersHelper{
  lazy val asys = ActorSystem.create("ProcessWrappersSpec")
}

trait ProcessWrappersSpecHelper extends ExecUtils{
  implicit def asys = ProcessWrappersHelper.asys
  def oneSecond = FiniteDuration(1, "second")

  def execSuccess() = exec("ls")
  def execFailure() = exec("scala", "-no-such-option")

}

class ProcessWrappersSpec extends Specification
  with ProcessWrappersSpecHelper with ProcessWrappers
{
  def is =                                                        s2""" ${"ProcessWrappers".title}
  `ProcessWrappers` trait provides
    `RunningProcess` and an implicit conversion from `Process`          $runningProcess
    `FinishedProcess` container
                                                                        """

  def runningProcess =                                                  s2"""
      A `RunningProcess` listens to process exit and creates a `FinishedProcess`, that can be accessed

        Synchronously                                                   $accessFinishedSync
        Asynchronously                                                  $accessFinishedAsync
                                                                        """

  def accessFinishedSync =                                              s2"""
          by method `await`                                             ${sync.await}
                                                                        """

  def accessFinishedAsync =                                             s2"""
          by method `onComplete`                                        ${async.complete}
          by methods `onSuccess`/`onFailure`                            ${async.successAndFail}
                                                                        """

  object async{
    def successAndFail = {
      var r1, r2 = ""

      val s = execSuccess()
      val f = execFailure()

      s.onSuccess(_ => r1 += "success"); s.onFailure(_ => r1 += "failure")
      f.onSuccess(_ => r2 += "success"); f.onFailure(_ => r2 += "failure")

      Await.ready(Future.sequence(Seq(s.asFuture, f.asFuture)), oneSecond)
      r1 === "success" and r2 === "failure"
    }

    def complete = {
      val r1, r2 = new StringBuilder

      val s = execSuccess()
      val f = execFailure()

      def foo(log: StringBuilder): FinishedProcess => Unit = {
        case p if p.success =>  log ++= "success"
        case _ =>               log ++= "failure"
      }

      s.onComplete(foo(r1))
      f.onComplete(foo(r2))

      Await.ready(Future.sequence(Seq(s.asFuture, f.asFuture)), oneSecond)
      r1.mkString === "success" and r2.mkString === "failure"
    }
  }

  object sync{
    def await = {
      val s = execSuccess()
      val f = execFailure()

      val e1 = s.await(oneSecond)
      val e2 = f.await(oneSecond)

      (e1.exitCode === 0) and (e2.exitCode !== 0)
    }
  }

}


