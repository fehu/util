package feh.util.file

import java.io.{FileInputStream, FileOutputStream, File => JFile}
import feh.util._
import scala.util.Try

trait FileUtilWrappers{
  self: FileUtils =>

  lazy val separatorChar = JFile.separatorChar
  
  // default string to path wrapper, uses File.separator to split the string
  implicit def stringToPath(string: String) = Path(string, separatorChar)
  
  implicit class FilePath(val path: Path) {
    def file = new JFile(path.toString)
    def delete() = Try{file.delete()}
  }
  
  implicit class ByteArrayToFileWrapper(arr: Array[Byte]){
    def toFile(file: String): JFile = toFile(new JFile(file))
    def toFile(file: JFile): JFile = file $$ (_.withOutputStream(_.write(arr)))
  }

  implicit class StringToFileWrapper(str: String){
    def toFile: JFile = new JFile(str)
  }

  implicit class FileWrapper(file: JFile){
    def withOutputStream[R](f: FileOutputStream => R, append: Boolean = false): Try[R] = {
      val stream = new FileOutputStream(file, append)
      Try(f(stream)) $$ { _ =>
        stream.flush()
        stream.close()
      }
    }
    def withInputStream[R](f: FileInputStream => R): Try[R] = {
      val stream = new FileInputStream(file)
      Try(f(stream)) $$ { _ =>
        stream.close()
      }
    }
    def mv(path: Path, `override`: Boolean = false) = Try{
      val dest = new JFile(path.toString)
      if(dest.exists()) if(`override`) dest.delete() else sys.error(s"destination file $dest exists")
      file.renameTo(dest)
      dest
    }

    def createIfNotExists() = Try{
      if(!file.exists()) createFile(Path.absolute(file.toString, separatorChar))
      file
    }

    def copy(file: JFile, overwrite: Boolean = false): Try[JFile] = cp(Path.absolute(file.toString, separatorChar), overwrite)
    def cp(path: Path, overwrite: Boolean = false): Try[JFile] = Try{
      val dest = path.file
      val parentsDir = path.tail
      if(!parentsDir.file.exists()) parentsDir.file.mkdirs().ensuring(b => b,"failed to create parent dirs")
      if(dest.exists()) if(overwrite) dest.delete() else sys.error(s"destination file $dest exists")
      dest.withOutputStream(write(file)).get
      dest
    }

    def affect(f: JFile => Unit) = Try{
      f(file)
      file
    }

    def existing[R](f: JFile => R): Option[R] = if(exists) Some(f(file)) else None

    def name = file.getName
    def ls(filter: JFile => Boolean = null) = file.listFiles().toList |>> (Option(filter), _.filter)
    def dir_? = file.isDirectory
    def isDir = file.isDirectory
    def exists = file.exists()
    def mkDir() = Try{
      file.mkdirs()
      file
    }

    def path = file.getAbsolutePath
  }
}