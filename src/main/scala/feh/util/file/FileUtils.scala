package feh.util.file

import java.io.{File => JFile, FileOutputStream, InputStream}
import feh.util._
import feh.util.Path.{EmptyPath, /}
import org.apache.commons.io.IOUtils
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.matching.Regex
import scala.util.{Success, Try}
import scala.collection.JavaConversions._

trait FileUtils {
  fileUtils =>

//  def File = this

  def apply(path: Path): JFile = new JFile(path.toString)

  def separatorChar = JFile.separatorChar
  def separator = JFile.separator
  def pathSeparatorChar = JFile.pathSeparatorChar
  def pathSeparator = JFile.pathSeparator

  def dropExt(filename: String) = {
    val i = filename.lastIndexOf("")
    if(i < 0) filename else filename.substring(0, i)
  }

  object pattern{
    def name(pattern: Regex): JFile => Boolean = pattern matches _.name
  }
  @tailrec
  final def readBytesFromStream(instream: InputStream, acc: ListBuffer[Byte] = ListBuffer.empty): Array[Byte] =
    if(instream.available > 0) readBytesFromStream(instream, acc += instream.read.toByte) else acc.toArray

  def read[R: Reader](is: InputStream): R = implicitly[Reader[R]].read(is)
  def write(file: JFile): FileOutputStream => Unit = {
    stream =>
      file.withInputStream(IOUtils.copy(_, stream))
  }
  def write(bytes: Array[Byte]): FileOutputStream => Unit = {
    stream =>
      IOUtils.write(bytes, stream)
      stream.flush()
  }

  object write{
    def utf8(str: String): FileOutputStream => Unit = write(str.getBytes("UTF-8"))
  }

  case class FileBuilder(dirPath: Path, isTemporary: Boolean)(val dir: JFile = dirPath.file){
    assert(dir.exists() && dir.isDirectory && dir.canWrite, "directory doesn't exist or isn't accessible")
    if(isTemporary) dir.deleteOnExit()

    def createFile(path: Path, `override`: Boolean = isTemporary): Try[JFile] = fileUtils.createFile(path append dirPath)
    def createDir(path: Path) = path.file.affect(_.mkdirs().ensuring(x => x, s"couldn't create dir $path"))
  }

  protected[util] def crFile(path: Path) = path.file //.log("creating file " + _).file
    .affect(_.createNewFile().ensuring(x => x, "failed to create file"))

  def createFile(path: Path, `override`: Boolean = false): Try[JFile] = {
    def inner(path: Path, first: Boolean = false): Try[JFile] = path match {
      case Exists(Dir(dir)) =>
        Success(dir.file)
      case Exists(file) =>
        if(`override`) file.delete() flatMap (_ => inner(file))
        else sys.error(s"file $file exists")
      case / / file if first =>
        val f = file
        crFile(path)
      case rest / file if first =>
        val f = file
        inner(rest).flatMap{
          _ => crFile(path)
        }
      case rest / dir =>
        path.file.mkDir()
    }
    inner(path, first = true)
  }

  def temporaryDir(name: String, deleteOnExit: Boolean = true): FileBuilder = {
    assert(!name.contains(separatorChar))
    val dir = temporary(name, deleteOnExit)
    if(dir.exists()) assert(dir.delete(), "couldn't remove old temp directory")
    dir.mkdir()
    dir.mv(name)
    FileBuilder(Path.relative(dir.toString), deleteOnExit)(dir)
  }

  def temporary(path: Path, deleteOnExit: Boolean = true): JFile = path match {
    case Path(EmptyPath, name, ext) => JFile.createTempFile(name, ext)
    case / / dir / file =>
      val b = temporaryDir(dir, deleteOnExit)
      b.createFile(file, `override` = true).get
    case s => sys.error(s"path larger then 2 isn't supported $s")
  }


  /**
   * extract unexisting part of path
   */
  object Unexisting{
    def unapply(path: Path): Option[Path] = {
      val p = path.tails.takeWhile(p => !p.file.exists()).toSeq
      val q = RelativePath(p.reverse.map(_.reversed.head), ' ')
      q.backOpt
    }
  }

  /**
   * extract unexisting part of path
   */
  object Existing{
    def unapply(path: Path): Option[Path] = path.tails.takeWhile(p => p.file.exists()).toSeq.lastOption
  }

  object Exists{
    def unapply(path: Path): Option[Path] = if(path.file.exists()) Some(path) else None
  }

  object Dir{
    def unapply(path: Path): Option[Path] = if(path.file.dir_?) Some(path) else None
  }

  object Resource{
    private def streamOpt(path: Path) = Option(getClass.getResourceAsStream(path.toString))

    /**
     * less safe then apply
     */
    def source(path: Path) = Option(getClass.getResource(path.toString)).map(Source.fromURL)
    def apply[R](path: Path)(f: InputStream => R): Try[R] = {
      val s = streamOpt(path)
        .orElse(streamOpt(path.toAbsolute.internal))
      Try{
        s.getOrElse(sys.error(s"no resource found: $path ")) |> f
      } $$ {
        s.foreach(_.close())
      }
    }
  }
}

trait Reader[+T]{
  def read: InputStream => T
}

trait FileReaders{
  val File: FileUtils

  implicit object ByteArrayReader extends Reader[Array[Byte]]{
    def read: InputStream => Array[Byte] = File.readBytesFromStream(_)
  }
  implicit object StringReader extends Reader[String]{
    def read: InputStream => String = is => new String(File.readBytesFromStream(is))
  }

  implicit object LinesReader extends Reader[Seq[String]]{
    def read: InputStream => List[String] = is => IOUtils.readLines(is).toList
  }
}

trait FileImplicits{
  self: FileUtilWrappers =>

  implicit object ByPathFileProvider extends ByPathProvider[JFile]{
    def apply(path: Path): JFile = File(path)
    def list(path: Path): Seq[String] = apply(path).list
    def listD(path: Path): Seq[JFile] = apply(path).listFiles
  }

  implicit object PreservingByPathFileProvider extends ByPathProvider[(Path, JFile)]{
    def apply(path: Path) = path -> File(path)
    def list(path: Path) = apply(path)._2.list
    def listD(path: Path) = apply(path)._2.listFiles.map(file => path / file.name -> file)
  }
}
