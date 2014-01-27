package feh.util

trait AbstractScopedState[T]{
  protected def default: T

  protected def state_=(t: T)

  def get: T
  def state = get

  def doWith[R](t: T, f: T => R): R = doWith(t)(f(t))
  def doWith[R](t: T)(r: => R): R = {
    val old = get
    state = t
    val res = r
    state = old
    res
  }
}

object AbstractScopedState{
  trait IgnoreUpdate[T] extends AbstractScopedState[T]{
    override def doWith[R](t: T)(r: => R): R =
      if(ignoreDo > 0) {
        ignoreDo -= 1
        r
      }
      else super.doWith(t)(r)

    /** ignore `doWith` state update
      *  numeric state
      */
    protected var ignoreDo = 0

    def ignoring[R](times: Int)(f: => R): R = {
      val old = ignoreDo
      ignoreDo += times
      val res = f
      ignoreDo = old
      res
    }
  }
}

trait ScopedInThreadState[T] extends AbstractScopedState[T]{
  private val _state = new ThreadLocal[T]{
    override def initialValue(): T = default
  }

  def get: T = _state.get
  protected def state_=(t: T) = _state.set(t)
}

class ScopedState[T](protected val default: T) extends ScopedInThreadState[T]
class ScopedStates[T](protected val default: Set[T]) extends ScopedInThreadState[Set[T]]{
  def doWithAdditional[R](t: Set[T])(r: => R): R = {
    val c = get.filter(t.contains)
    state = get ++ t
    val res = r
    state = get -- t ++ c
    res
  }
//  def doWithout[R](t: Set[T])(r: => R): R = ???

}