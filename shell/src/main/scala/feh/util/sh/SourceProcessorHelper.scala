package feh.util.sh

import scala.util.matching.Regex
import scala.collection.mutable.ListBuffer

trait SourceProcessorHelper {
  def aggregateLines(code: String, from: Int): Int ={
    val i = code.indexOf('\n', from)
    if(i == -1) code.length
    else if (code.substring(from, i).trim.lastOption.exists(_ == '\\')) aggregateLines(code, i+1)
    else i

  }

  def allMatchesWithAggregatedLines(code: String, regex: Regex): List[(Int, Int)] =
    regex.findAllMatchIn(code).toList.map{
      m =>
        val end = aggregateLines(code, m.start)
        (m.start, end)
    }

  /**
   * @return (replaced, extracted)
   */
  def extractAndReplace(code: StringBuilder, segments: List[(Int, Int)])
                       (segmentStartLen: Int, segmentEndLen: Int)
                       (replace: String => Option[String]): (StringBuilder, List[String]) = {
    val acc = ListBuffer.empty[String]

    segments.sortBy(_._1).reverse.foreach{
      case (start, end) =>
        val orig = code.substring(start+segmentStartLen, end)
        acc += orig
        replace(orig).foreach( code.replace(start, end+segmentEndLen, _) )
    }
    code -> acc.toList
  }

}

object SourceProcessorHelper extends SourceProcessorHelper
