package feh.util

import scala.collection.SeqLike
import scala.util.Random

trait RandomWrappers {
  implicit class SeqLikeWrapper[+A, +Repr](seq: SeqLike[A, Repr]){
    def randomChoice: Option[A] = if(seq.nonEmpty) Some(seq(Random.nextInt(seq.length))) else None
    def randomOrder: Seq[A] = seq.toSeq.map(Random.nextInt() -> _).sortBy(_._1).map(_._2)
    def randomPop: Option[(A, Seq[A])] = {
      lazy val i = Random.nextInt(seq.length)
      lazy val sq = seq.toSeq

      if(seq.nonEmpty) Some(seq(i), sq.take(i) ++ sq.drop(i+1)) else None
    }
  }

  implicit class MapLikeWrapper[A, +B, +This <: scala.collection.MapLike[A, B, This] with scala.collection.Map[A, B]](mlike: scala.collection.MapLike[A, B, This]){
    def randomChoice: Option[(A, B)] = {
      mlike.keys.toList.randomChoice map {
        k =>
          k -> mlike(k)
      }
    }
  }

  implicit class SetLikeWrapper[A, Repr <: collection.SetLike[A, Repr] with Set[A]](set: collection.SetLike[A, Repr]){
    def randomChoice: Option[A] = set.toSeq.randomChoice
  }

  implicit class RangeWrapper(r: Range){
    def randomSelect: Int = {
      val n = Random.nextInt(r.length)
      r.min + r.step*n
    }
  }
}

object RandomWrappers extends RandomWrappers