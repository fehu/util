package feh.util.scutil

import scala.swing.{Frame, EditorPane}
import scala.tools.nsc.interpreter.{Parsed, JLineCompletion, IMain}
import scala.swing.event.{Key, KeyPressed}
import javax.swing.text.Utilities
import scala.swing.Swing._
import feh.util._
import feh.util.scutil.CompletionEditorPane.{CtrlShiftSpaceCompletion, Impl, CtrlSpaceCompletion, AbstractCompletionEditorPane}

trait CompletionProvider {
  def complete(text: String, position: Int, verbosity: Option[Int]): List[String]
}

class ScalaCompletionProvider(protected val iMain: IMain) extends CompletionProvider with IMainProvider{
  protected val completion = new JLineCompletion(iMain)

  case class YParam(reversedInput: List[Char], result: StringBuilder, previous: Char, state: State)
  case class State(insideStringLiteral: Boolean = false, inWord: Boolean = false, var spaceToDotReplaced:Int = 0)
  case class CurrentFragment(fragment: String, position: Int)

  def getCurrentFragment(text: String, position: Int) ={
    val substrStart = text.substring(0, position)
    val substrEnd = text.substring(position).takeWhile(_.isLetterOrDigit)
//    println(s"substrStart=$substrStart, substrEnd=$substrEnd")
    val substr = substrStart ++ substrEnd
    val posFromEnd = substrEnd.length

    val fr = Y[YParam, String](
      rec => {
        case YParam(h :: t, res, prev, state) if h.isLetterOrDigit =>
//          println(s"YParam: h.isLetterOrDigit: h=$h, res=$res, prev=$prev, state=$state")
          prev match{
            case '.' => res += '.'
            case ' ' if state.spaceToDotReplaced == 0 => 
              state.spaceToDotReplaced += 1
              res += '.'
            case ' ' => res.reverse.mkString
            case _ => 
          }
          if(state.inWord) rec(YParam(t, res += h, h, state))
          else rec(YParam(t, res += h, h, state.copy(inWord = true)))
        case YParam(h :: t, res, prev, state) if !state.insideStringLiteral && (h.isWhitespace || h == '.') =>
//          println(s"YParam: h.isWhitespace || h == '.': h=$h, res=$res, prev=$prev, state=$state")
          if(state.inWord) rec(YParam(t, res, h, state.copy(inWord = false)))
          else h match{
            case '.' if prev == '.' => res.reverse.mkString                                                             //some kind of error - return
            case _ => rec(YParam(t, res, if(prev == '.') prev else h, state))
          }
        case YParam(Nil, res, _, state) =>
//          println(s"YParam: Nil: res=$res, state=$state")
          res.reverse.mkString
        case x@YParam(_, res, _, _) =>
//          println(s"YParam: other: $x")
          res.reverse.mkString
      }
    )(
      YParam(substr.reverse.toList, StringBuilder.newBuilder, '_', State())
    )

    CurrentFragment(fr, fr.length - posFromEnd)
  }


  def complete(text: String, position: Int, verbosity: Option[Int]) = {
    val CurrentFragment(fr, newPosition) = getCurrentFragment(text, position)
//    println(s"fr=$fr, newPosition=$newPosition")
    val parsed = Parsed.dotted(fr, newPosition)
    completion.topLevelFor(verbosity.map(parsed withVerbosity) getOrElse parsed)
  }
}

object CompletionEditorPane{
  trait AbstractCompletionEditorPane extends EditorPane{
    def completion: CompletionProvider
  }

  trait ListeningKeys{
    pane: AbstractCompletionEditorPane =>

    listenTo(keys)

    def complete(verbosity: Option[Int], action: List[String] => Unit){
      completion.complete(pane.text, pane.caret.dot, verbosity) match {
        case Nil =>
        case list => action(list.distinct)
      }
    }
  }

  trait CompletionPopup{
    pane: AbstractCompletionEditorPane =>

    def popup(position: Int, canComplete: List[String])
  }

  trait CtrlSpaceCompletion extends ListeningKeys with CompletionPopup{
    pane: AbstractCompletionEditorPane =>

    def ctrlSpaceVerbosity: Option[Int]

    reactions +={
      case KeyPressed(src, Key.Space, Key.Modifier.Control, _) =>
        complete(ctrlSpaceVerbosity, popup(pane.caret.dot, _))
    }
  }
  
  trait CtrlShiftSpaceCompletion extends ListeningKeys with CompletionPopup{
    pane: AbstractCompletionEditorPane =>

      def ctrlShiftSpaceVerbosity: Option[Int]

      private val CtrlShiftModifier = Key.Modifier.Control + Key.Modifier.Shift

      reactions +={
        case KeyPressed(src, Key.Space, CtrlShiftModifier, _) =>
          complete(ctrlShiftSpaceVerbosity, popup(pane.caret.dot, _))
      }
  }

  object Impl{
    @deprecated("temporary")
    trait LabelCompletionPopup extends CompletionPopup{
      pane: AbstractCompletionEditorPane =>

      def popupYOffset: Int = 3

      def popup(position: Int, canComplete: List[String]){
        val charRect = pane.peer.getUI.modelToView(pane.peer, position)
        val popupPos = charRect.x -> (charRect.y + charRect.height + popupYOffset)

        println(s"canComplete = $canComplete")
        println(s"popupPos = $popupPos")
//        val
      }
    }
  }
}

class CompletionEditorPane(val completion: CompletionProvider)
  extends AbstractCompletionEditorPane with Impl.LabelCompletionPopup
  with CtrlSpaceCompletion with CtrlShiftSpaceCompletion
{
  def ctrlShiftSpaceVerbosity = Some(10)
  def ctrlSpaceVerbosity = None
}

class LegacyCompletionEditorPane(iMain: IMain) extends EditorPane{
  protected val completion = new JLineCompletion(iMain)
//  protected val completer = completion.completer()

//  val ek = this.editorKit
//  ek.
  listenTo(keys)
  reactions += {
    case KeyPressed(src, Key.Space, Key.Modifier.Control, _) =>
      println(src.getClass)
      val dot = caret.dot
      val st = Utilities.getWordStart(peer, dot)
      val en = Utilities.getWordEnd(peer, dot)
      val sel = text.substring(st, en)
      println(s"sel=$sel, dot=$dot, st=$st, en=$en, dot-st=${dot-st}, sel='$sel', text='$text', ${sel == text}")
      val candidates = completion.completions(sel)
      val c = completion.completer()
      println(c.complete(sel, dot))
      for(i <- 0 until 10) println(i + ":  " + c.complete(sel, i))
      for(i <- 0 until 10) println(i + "^:  " + c.complete(text, i))
      println(candidates)
      println(completion.completions(text))
      completion.lastResult
      println("parsed0: " + Parsed(sel))
      println("parsed1: " + Parsed(sel, dot))
      println("parsed2: " + Parsed.undelimited(sel, dot))
      println("parsed3: " + Parsed.dotted(sel))
      println("parsed4: " + Parsed.dotted(sel, dot))
      println("parsed4: " + Parsed(sel, 1, Parsed.DefaultDelimiters + '+'))
      println("==")
    case KeyPressed(src, Key.Space, mod, _) if mod == Key.Modifier.Control + Key.Modifier.Shift =>
      val dot = caret.dot
      val st = Utilities.getWordStart(peer, dot)
      val en = Utilities.getWordEnd(peer, dot)
      val sel = text.substring(st, en)
      val p = Parsed.dotted(sel, dot) withVerbosity 10
//      println(s"## p=${p.}")
      println(s"## dot=$dot  " + completion.topLevelFor(p))
      for(i <- 0 until 10) println(i + "#:  " + completion.topLevelFor(Parsed.dotted(sel, i) withVerbosity 10))
  }

}

object CompletionEditorPaneApp extends App with DefaultIMain{
  val pane = new LegacyCompletionEditorPane(iMain)
  val fr = new Frame{
    title = "LegacyCompletionEditorPane"
    contents = pane
    size = 600 -> 300
  }

  fr.open()

  val pane2 = new CompletionEditorPane(new ScalaCompletionProvider(iMain))
  val fr2 = new Frame{
    title = "CompletionEditorPane"
    contents = pane2
    size = 400 -> 300
  }

  fr2.open()
}

object CompletionTest extends App with DefaultIMain{
  lazy val completion = new JLineCompletion(iMain)

  iMain.initializeSynchronous()
  val completer = completion.completer()
  println(completer.complete("In", 0))
  println(completer.complete("In", 1))
  println(completer.complete("In", 2))
  println(completer.complete("In", 3))
  println(completer.complete("In", 4))
  println(completer.complete("In", 5))
  println(completer.complete("In", 6))

}

/*
class ScalaCompletionProviderOld extends CompletionProvider{
  def searchWhileDotted = ???

  case class YParam(reversedInput: List[Char], result: StringBuilder, previous: Char,state: State)
  case class State(insideStringLiteral: Boolean = false)

  def getCurrentFragment(text: String, position: Int) =
    Y[YParam, String](
        rec => {
          case YParam(h :: t, res, prev, state) if h.isLetterOrDigit =>
//          case ('\n' :: t, acc) if h.is => ""
        }
      )(
        text.reverse.toList -> StringBuilder.newBuilder
      )
//    StringBuilder.newBuilder.append(text).reverse
//      .dropWhile(bool.oneOf(_.isWhitespace, includePreviousLine && _ == '\n')).takeWhile('\n' !=)

  def currentExpr(text: String, position: Int) = {
    val fragment = getCurrentFragment(text, position)
    text.charAt(position) match {
      case '.' | ' ' => membersFor(fragment)
//      case
    }
  }

  def membersFor(fragment: String) = {
    getCurrentFragment()
  }

//  Y[(List[Char], StringBuilder), String](
//      rec => {
//        case (h :: t, acc) if h.is => ""
//      }
//    )

  def complete(text: String, position: Int, verbosity: Int) = ???
}

 */