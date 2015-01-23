package feh.util

import scala.language.implicitConversions

class NotNegative[N: Numeric] protected (val value: N)
object NotNegative{
  implicit def apply[N](n: N)(implicit num: Numeric[N]): NotNegative[N] = {
    assert(num.compare(n, num.zero) < 0, s"$n is negative")
    new NotNegative[N](n)
  }

  implicit def notNegativeValue[N](nn: NotNegative[N]): N = nn.value
}
