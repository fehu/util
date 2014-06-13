package feh.util.exec

import feh.util._
import akka.actor.ActorDSL._
import scala.concurrent.{Future, Promise, Await}
import scala.collection.mutable.ListBuffer
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.util.Success

trait ProcessWrappers{

  sealed trait WrappedProcess{
    def process: Process
    def execFinished: Boolean
  }

  implicit def runningProcessWrapper(p: Process)(implicit asys: ActorSystem) = new RunningProcess(p)

  class RunningProcess protected[ProcessWrappers] (val process: Process)(implicit val asys: ActorSystem) extends WrappedProcess{
    protected var finished_? = false
    def execFinished = finished_?

    protected var _finished: FinishedProcess = _
    def finished = Option(_finished)

    protected def beforeAwait(){}
    protected def afterAwait(){}

    protected case class OnCompletion(f: FinishedProcess => Unit)

    protected val overseer = actor(new Act {
      val onCompletion = ListBuffer.empty[FinishedProcess => Unit]

      case object Finished

      become {
        case OnCompletion(f) =>
          if(!finished_?) onCompletion += f
          else f(_finished)
        case Finished =>
          assert(!finished_?)
          finished_? = true
          _finished = createFinishedProcess()
          onCompletion foreach (_(_finished))
          promise.complete(Success(_finished))
      }

      Future{
        beforeAwait()
        process.waitFor()
        afterAwait()
        self ! Finished
      }(asys.dispatcher)
    })

    protected def createFinishedProcess() = new FinishedProcess(process)

    protected lazy val promise = Promise[FinishedProcess]()
    def asFuture = promise.future

    def await(timeout: FiniteDuration): FinishedProcess = {
      Await.ready(asFuture, timeout)
      finished.get
    }

    def onComplete(f: FinishedProcess => Unit) { overseer ! OnCompletion(f) }
    def onSuccess(f: FinishedProcess => Unit) { onComplete( _.cond(_.success) foreach f ) }
    def onFailure(f: FinishedProcess => Unit) { onComplete( _.cond(_.failure) foreach f ) }
  }

  object RunningProcess{
    implicit def toProcess(fp: RunningProcess) = fp.process
  }

  class FinishedProcess protected[ProcessWrappers] (val process: Process) extends WrappedProcess{
    final def execFinished = true

    def exitCode = process.exitValue()
    def success = exitCode == 0
    def failure = !success

    def map[R](f: FinishedProcess => R): R = f(this)
    def cond(f: FinishedProcess => Boolean): Option[FinishedProcess] = this.inCase(f)
  }

  object FinishedProcess{
    implicit def toProcess(fp: FinishedProcess) = fp.process
  }

}

object ProcessWrappers extends ProcessWrappers