package feh.util


import java.io.File

import feh.util.Path.EmptyPath
import feh.util.PathSelector.{WithFilter, All}

sealed trait Path{
  def separatorChar: Char
  def reversed: List[String]
  def absolute: Boolean
  protected def cpy(path: List[String]): Path
  def isEmpty = reversed.isEmpty
  def length = reversed.length

  lazy val path = reversed.reverse
  def /(next: String): Path = cpy(next :: reversed)
  def /(next: RelativePath): Path = prepend(next)
  def back =
    if(length != 1) cpy(reversed.tail)
    else EmptyPath
  def backOpt = {
    if(length > 0) Some(back) else None
  }
  def prepend(next: Path) = cpy(next.reversed ::: this.reversed)
  def append(next: Path) = cpy(this.reversed ::: next.reversed)

  def name = reversed.head
  def ext = splittedName._2
  def splittedName = {
    val split = name.split('.')
    if (split.length == 1) split.head -> ""
    else split.dropRight(1).mkString("") -> split.last
  }

  def intersects(that: Path) =  ???

  def head = name
  def tail = back

  def rHead = reversed.last
  def rHeadOpt = reversed.lastOption
  def rTail = cpy(reversed.dropRight(1))
  def tails: Iterator[Path] =
    if(length == 1) Iterator(this)
    else reversed.tails.map(p => Path(p.reverse, separatorChar, absolute))

  override def equals(obj: scala.Any): Boolean = PartialFunction.cond(obj) {
    case p: Path if p.absolute == this.absolute && p.reversed == this.reversed => true
  }
  def toAbsolute: AbsolutePath
  def toRelative: RelativePath

  def mkString(sep: String) = path.mkString(sep)

  def p = this

  def drop(path: Path) =
    if(reversed endsWith path.reversed) RelativePath(reversed.dropRight(path.length).reverse, separatorChar)
    else sys.error(s"$this doesn't start with $path")
}

trait PathImplicits{
  implicit def toString(path: Path) = path.toString

  implicit class StringToPath(str: String){
    def pathBy(separator: Char): RelativePath = RelativePath(str.split(separator), separator)

    def pathBy(start: String, separator: Char): AbsolutePath = str.trim.ensuring(_.startsWith(start), "isn't absolute")
      .drop(start.length).split(separator).toList |> (AbsolutePath(_, separator))
  }

  implicit def pathSelectorWrapper(path: Path) = PathSelector(path)
  implicit class PathSelectorWrapper(path: Path){
    def /(next: PathSelector): PathSelector = next.copy(path / next.rootPath.toRelative)
    def /(a: *.type): PathSelector = PathSelector.all(path)
    def /?(filter: String => Boolean): PathSelector = PathSelector(path, WithFilter(filter) :: Nil)
  }

  case object *

}

object Path extends PathImplicits{

  def absolute(path: String, separatorChar: Char = File.separatorChar): AbsolutePath = AbsolutePath(path, separatorChar)
  def relative(path: String, separatorChar: Char = File.separatorChar): RelativePath = RelativePath(path, separatorChar)

  def apply(list: List[String], separatorChar: Char, absolute: Boolean): Path =
    if(absolute) AbsolutePath(list, separatorChar) else RelativePath(list, separatorChar)
  def apply(str: String, separatorChar: Char): Path = {
    assert(str.nonEmpty && str != null, "path is empty")
    val (s, abs) = if(str(0) == separatorChar) str.drop(1) -> true else str -> false
    if(abs) AbsolutePath(s.split(separatorChar), separatorChar)
    else if(s(0) == '.' && s.substring(1).headOption == Some(separatorChar)) RelativePath(s.drop(2).split(separatorChar), separatorChar)
    else RelativePath(s.split(separatorChar), separatorChar)
  }
  // path, file, suffix
  def unapply(path: Path): Option[(Path, String, String)] =
    Some(path.back, path.splittedName._1, path.splittedName._2)

  def group(paths: Seq[Path]): Seq[(Path, Seq[String])] = paths
    .map{ case p / imp => (p, imp) }
    .groupBy(_._1).toSeq.map{
    case (p, iseq) => p -> iseq.map(_._2)
  }

  /**
   * Use only for pattern matching
   */
  sealed trait EmptyPath extends Path{
    def emptyPathException(call: String) = sys.error(s"calling $call on EmptyPath")

    override def isEmpty = true
    def reversed = Nil
    def absolute: Boolean = emptyPathException("absolute")
    protected def cpy(path: List[String]): Path = sys.error(s"use EmptyPath.Relative or EmptyPath.Absolute")
    override def length = 0
    override def backOpt = None
    override def back = emptyPathException("back")
    override def name = ""
    override def ext = ""
    override def splittedName = "" -> ""
    override def head = emptyPathException("head")
    override def tail = emptyPathException("tail")
    override def tails = Iterator.empty


    override def equals(obj: Any) = obj match{
      case p: Path if p.isEmpty => true
      case _ => false
    }
  }
  object EmptyPath extends EmptyPath{
    def separatorChar = File.separatorChar

    object Absolute extends AbsolutePath(Nil) with Absolute

    trait Absolute extends AbsolutePath with EmptyPath {
      override val reversed = Nil
      override val absolute = true
      override protected def cpy(path: List[String]) = AbsolutePath(path, separatorChar)
      override def /(next: RelativePath) = AbsolutePath(next: String)
      override def /(next: String) = AbsolutePath(next)
    }
    object Relative extends RelativePath(Nil) with Relative
    trait Relative extends RelativePath with EmptyPath {
      override val reversed = Nil
      override val absolute = false
      override protected def cpy(path: List[String]) = RelativePath(path, separatorChar)
      override def /(next: RelativePath) = RelativePath(next)
      override def /(next: String) = RelativePath(next)
    }

    def toAbsolute = Absolute
    def toRelative = Relative
  }

  object \ {
    def unapply(path: Path): Option[(String, Path)] = path.rHeadOpt map (_ -> path.rTail)
  }

  object / extends EmptyPath{

    def separatorChar = File.separatorChar

    def unapply(path: Path): Option[(Path, String)] = {
      path.backOpt map (_ -> path.name)
    }

    override def equals(obj: Any): Boolean = EmptyPath == obj

    override def / (p: String) = AbsolutePath(p)

    def toAbsolute = EmptyPath.Absolute
    def toRelative = EmptyPath.Relative
  }

  object `.` {
    def / (p: String) = RelativePath(p)
  }

}


trait PathBuildHelper{
  protected def build(path: Seq[String], separatorChar: Char) = path.flatMap(_.split(separatorChar)).map(_.trim).filter(_.nonEmpty)
}

object RelativePath extends PathBuildHelper{

  def apply(path: Seq[String], separatorChar: Char): RelativePath = build(path, separatorChar) match {
    case Nil => EmptyPath.Relative
    case p => new RelativePath(p.toList.reverse, separatorChar)
  }
  def apply(path: String, separatorChar: Char = File.separatorChar): RelativePath = apply(path :: Nil, separatorChar)
}
class RelativePath protected[util](val reversed: List[String], val separatorChar: Char = File.separatorChar) extends Path{
  def absolute = false
  protected def cpy(path: List[String]) = new RelativePath(reversed = path, separatorChar)

  def relToCurrentDir = "" + separator + toString

  override def toString: String = path.mkString(separator)

  def toAbsolute = new AbsolutePath(reversed, separatorChar)
  def toRelative = this

  def separator = separatorChar.toString
}

object AbsolutePath extends PathBuildHelper{

  def apply(path: Seq[String], separatorChar: Char): AbsolutePath = build(path, separatorChar) match {
    case Nil => EmptyPath.Absolute
    case p => new AbsolutePath(p.toList.reverse, separatorChar)
  }
  def apply(path: String, separatorChar: Char = File.separatorChar): AbsolutePath = apply(path :: Nil, separatorChar)
}

class AbsolutePath protected[util](val reversed: List[String], val separatorChar: Char = File.separatorChar) extends Path{
  def absolute = true
  protected def cpy(path: List[String]) = new AbsolutePath(reversed = path, separatorChar)

  override def toString: String = path.mkString(separator, separator, "")
  def internal = path.mkString("/", "/", "")

  def toAbsolute = this
  def toRelative = new RelativePath(reversed, separatorChar)

  def separator = separatorChar.toString
}
