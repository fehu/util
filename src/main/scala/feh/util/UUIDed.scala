package feh.util

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait HasUUID {
  val uuid: UUID
}

object HasUUID{
  implicit class HasUUIDFutureWrapper[T <: HasUUID](f: Future[T]){
    def havingSameUUID(other: HasUUID)(implicit context: ExecutionContext) = f.withFilter(_.uuid == other.uuid)
  }

  implicit class HasUUIDOptionWrapper[T <: HasUUID](opt: Option[T]){
    def havingSameUUID(other: HasUUID) = opt.filter(_.uuid == other.uuid)
  }
}

class UUIDed(final val uuid : UUID = UUID.randomUUID()) extends HasUUID