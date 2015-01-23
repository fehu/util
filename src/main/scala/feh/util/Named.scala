package feh.util

import scala.language.implicitConversions

case class Named[+T](name: String, value: T)
object Named{
  implicit class Create[T](value: T){
    def named(name: String) = Named(name, value)
  }
  implicit def get[T](named: Named[T]): T = named.value
}