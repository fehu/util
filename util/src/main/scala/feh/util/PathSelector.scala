package feh.util

import PathSelector._
import feh.util.Path.EmptyPath

case class PathSelector(rootPath: Path, selection: List[Selection] = Nil) {

  def select(path: RelativePath) = copy(selection = selection :+ PathSelection(PathSelector(path)))
  def select(path: PathSelection) = copy(selection = selection :+ path)
  def select(filter: String => Boolean) = copy(selection = selection :+ WithFilter(filter))
  def selectAll() = copy(selection = selection :+ All)

  def get[D: ByPathProvider]: List[D] = get_inner(implicitly[ByPathProvider[D]], EmptyPath.Absolute)

  protected def get_inner[D](byPath: ByPathProvider[D], prefix: Path): List[D] = selection flatMap {
    case PathSelection(sel@PathSelector(EmptyPath, Nil)) => Nil
    case PathSelection(PathSelector(path, Nil)) => byPath(prefix / rootPath / path) :: Nil
    case PathSelection(sel) => sel.get_inner(byPath, prefix / rootPath)
    case WithFilter(filter) => byPath.list(prefix / rootPath).filter(filter).map(prefix / rootPath / _ |> byPath.apply)
    case All => byPath.listD(prefix / rootPath)
  }
}

object PathSelector{
  def empty = PathSelector(EmptyPath)

  trait Selection

  case object All  extends Selection
  case class PathSelection(sel: PathSelector) extends Selection
  case class WithFilter(filter: String => Boolean)  extends Selection
}

trait ByPathProvider[D]{
  def apply(path: Path): D
  def list(path: Path): Seq[String]
  def listD(path: Path): Seq[D]
}
