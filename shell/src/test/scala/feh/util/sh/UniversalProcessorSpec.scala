package feh.util.sh

import org.specs2.Specification
import scala.collection.mutable
import scala.util.Try
import feh.util._
import org.specs2.execute.Result

object UniversalProcessorSpec{
  def uprocessor = UniversalProcessor.get

  def process = uprocessor.process(_: mutable.StringBuilder)
}

class UniversalProcessorSpec extends Specification{
  def is =                                                                        s2""" ${ "UniversalProcessor".title }
    A `UniversalProcessor` processes source code, reading its configuration from the same source.
    Provides the following features:                                                    $end
        | sh-line |   processes one-line shell expressions                              ${parse().shLine}
        the 'one-line' shell expressions support '\' multi-lining                       ${parse().shLineMulti}
        | sh-block |  processes multi-line shell expression blocks                      ${parse().shBlock}

            | shortcuts | provide the following shortcuts for scala expressions         $end
                var:    $$name = ...      => var name = ...                             ${parse().shortVar}
                val:    c$$name = ...     => val name = ...                             ${parse().shortVal}
                args:   $$1, $$2, ..., $$N  => args(1), args(2), ..., args(N)           ${parse().shortArgs}
                object: ##name           => object name                                 ${parse().shortObject}
            shortcuts must not affect strings and the following expressions             $end
                var:    "$$arg = $$value"                                               $todo
                val:    "c$$arg = c$$value"                                             $todo
                args:   evidence$$1, evidence$$2                                        $todo
                object: x.## max 2, "##ERROR"                                           $todo
            Multiline config:                                                           $end
                several #conf keywords in the begining of the source                    ${parse().allHeader}
                several #conf keywords in different parts of the source                 ${parse().allAnywhere}
                multi-line, escaped by '\'                                              $todo
        | all |       key for enabling all the features listed above                    ${parse().allKey}

            Dependency management                                                       $end
                by package *name*, *group* and *version*                                $todo
                by package *name* and *group*, choosing latest version                  $todo
                by package *name* only                                                  $todo
                all dependency management methods support scala versioning              $todo

        Quick imports                                                                   $todo

        Predefined imports                                                              $todo

        Predefined dependencies                                                         $todo
                                                                                        """

  import UniversalProcessorSpec._

  case class parse(){
    def shLine      = tt(process, 1, ExpectedTransforms.shLine)
    def shLineMulti = tt(process, 1, ExpectedTransforms.shLineMulti)
    def shBlock     = tt(process, 1, ExpectedTransforms.shBlock)
    def shortVar    = short.variable
    def shortVal    = short.const
    def shortArgs   = short.arguments
    def shortObject = short.`object`
    def allHeader   = tt(process, 4, ExpectedTransforms.all)
    def allAnywhere = (testTransform _).tupled(ExpectedTransforms.merged)(process)
    def allKey      = tt(process, 1, ExpectedTransforms.allKey)

    val short = Short(process)
    
    def shSurround(sh: String) = "_root_.feh.util.shell.Exec(\"\"\"" + sh + "\"\"\")"

    def shLineHeader =                  "#conf sh-line"
    def shLineExpr =                    "#sh ls -al | grep scala"
    def shLineExpected =   shSurround(  "ls -al | grep scala"  )

    def shBlockHeader =                 "#conf sh-block"
    def shBlockExpr =                 """#sh>
                                        |   if [ x -ne 0 ]; then res=true
                                        |   else res=false
                                        |   fi
                                        |<sh#""".stripMargin
    def shBlockExpected = shSurround( """
                                        |   if [ x -ne 0 ]; then res=true
                                        |   else res=false
                                        |   fi
                                        |""".stripMargin)

    def shortcutsHeader =               "#conf shortcuts"
    def shortcutVal =                   "c$g = 9.80665" -> "val g = 9.80665"
    def shortcutVar =                   "$count   =  0" -> "var count =  0"
    def shortcutArg =                   "$1 +  $2"      -> "args(1) +  args(2)"
    def shortcutObj =                 """
                                        |##settings{
                                        |  var DEBUG = false
                                        |}
                                     |""".stripMargin -> """
                                                           |object settings{
                                                           |  var DEBUG = false
                                                           |}
                                                        |""".stripMargin
    
    def shortcuts = shortcutVal :: shortcutVar :: shortcutArg :: shortcutObj :: Nil
    def shortcutsExpr = shortcuts.map(_._1).mkString("\n")
    def shortcutsExpect = shortcuts.map(_._2).mkString("\n")

    def shLineMultiExpr =             """
                                        |#sh mvn archetype:generate \
                                        |      -DarchetypeGroupId=org.scala-tools.archetypes \
                                        |      -DarchetypeArtifactId=scala-archetype-simple  \
                                        |      -DremoteRepositories=http://scala-tools.org/repo-releases \
                                        |      -DgroupId=org.glassfish.samples \
                                        |      -DartifactId=scala-helloworld \
                                        |      -Dversion=1.0-SNAPSHOT
                                     |""".stripMargin
    def shLineMultiExp = "\n" +
                         shSurround(  """mvn archetype:generate \
                                        |      -DarchetypeGroupId=org.scala-tools.archetypes \
                                        |      -DarchetypeArtifactId=scala-archetype-simple  \
                                        |      -DremoteRepositories=http://scala-tools.org/repo-releases \
                                        |      -DgroupId=org.glassfish.samples \
                                        |      -DartifactId=scala-helloworld \
                                        |      -Dversion=1.0-SNAPSHOT""".stripMargin) +
                                                                  """
                                     |""".stripMargin

    def allConf = "#conf #all"

    object ExpectedTransforms{
      def shLine      = (shLineHeader,        shLineExpr,         shLineExpected)
      def shBlock     = (shBlockHeader,       shBlockExpr,        shBlockExpected)
      def shortcuts   = (shortcutsHeader,     shortcutsExpr,      shortcutsExpect)
      def shLineMulti = (shLineHeader,        shLineMultiExpr,    shLineMultiExp)

      def allLines = ((eSeq, eSeq, eSeq) /: Seq(shLine, shBlock, shortcuts, shLineMulti)){
        case ((accH, accE, accX), (h, e, x)) => (accH :+ h, accE :+ e, accX :+ x)
      }

      def all = (allLines._1.mkString("\n"), allLines._2.mkString("\n"), allLines._3.mkString("\n"))
      def allKey = (allConf, all._2, all._3)

      def merged = Seq(shLine, shBlock, shortcuts, shLineMulti).map{
        case (h, e, x) => h + "\n" + e -> ("\n" + x)
      }.unzip match{
        case (src, expected) => src.mkString("\n") -> expected.mkString("\n")
      }

      private def eSeq = Seq.empty[String]
    }

    protected def tt(process: StringBuilder => StringBuilder, prependNewLines: Int, tuple: (String, String, String)) =
      testTransform(tuple._1 + "\n" + tuple._2, "\n"*prependNewLines + tuple._3)(process)
    protected def getTransform(source: String, process: StringBuilder => StringBuilder) = {
      val src = new StringBuilder(source)
      process(src).mkString
    }
    protected def testTransform(source: String, expected: String)(process: StringBuilder => StringBuilder) = {
      getTransform(source, process) mustEqual expected
    }

    case class Short(process: StringBuilder => StringBuilder) {
      protected lazy val transformQ = mutable.Queue(transform: _*)
      protected var failed = false

      lazy val const        = testTransform(shortcutVal._2)
      lazy val variable     = testTransform(shortcutVar._2)
      lazy val arguments    = testTransform(shortcutArg._2)
      lazy val `object`     = testTransform(shortcutObj._2) and noMore

      protected def testTransform(expected: String): Result =
        if(failed) skipped("skipped due to previous test failure")
        else {
          val l = lines(expected)
          val n = l.length
          takeQ(n) $${
            _.failed.foreach(_ => failed = true)
          } must beSuccessfulTry(l)
        }

      protected def noMore = transformQ must beEmpty
      protected def takeQ(n: Int) = Try{ takeQueue(n) }
      protected def takeQueue(n: Int): Seq[String] =
        if(n == 0) Nil
        else transformQ.dequeue() +: takeQueue(n-1)
      protected def transform = lines(getTransform(shortcutsHeader + "\n" + shortcutsExpr, process))

      private def lines(str: String) = str.split('\n').toList

      // drop the first line, left by conf header
      transformQ.dequeue()
      // init in the correct order
      const; variable; arguments; `object`
    }
  }

  case class dep(){

  }
}
