package feh.util.sh.exec

import feh.util.FileUtils.{AbsolutePath, Path}
import feh.util.ScopedState
import scala.util.Try
import java.io.File

sealed trait ImportInfo
  case class LibInfo(group: String, name: String, version: String, meta: LibInfoMeta) extends ImportInfo
  case class Import(path: Path, wildcard: Boolean, libs: Seq[LibInfo]) extends ImportInfo
  {
    def transform(fpath: Path => Path = identity,
                  wildcard: Boolean = this.wildcard,
                  flibs: Seq[LibInfo] => Seq[LibInfo] = identity
                   ) =
      Import(fpath(path), wildcard, flibs(libs))
  }

object ImportInfo{
  implicit class ImportInfoSeqOps(ii: Seq[ImportInfo]){
    def extractLibs = ii.flatMap{
      case lib: LibInfo => Seq(lib)
      case Import(_,_,libs) => libs
    }
  }
}

trait LibInfoMeta
  case class Managed(dependsOnScala: Boolean) extends LibInfoMeta
  case class Unmanaged(path: Path) extends LibInfoMeta
case class LibPath(inf: LibInfo, path: Path)

case class ClassPath(libs: LibPath*){
  def paths = libs.map(_.path)
  def asString = paths.mkString(File.pathSeparator)
  override def toString = s"ClassPath($asString)"
}
case class ScalaVersion(version: String){
  def complies(v: String) = version == "_" || v == version || (v.startsWith(version) && !v.contains("-"))
}

trait ClassPathResolver{
  def resolvePath(inf: LibInfo): LibPath
  def searchIvy(inf: LibInfo): Option[Path]
  def searchMaven(inf: LibInfo): Option[Path]

  def classpath(libs: LibInfo*): ClassPath = ClassPath(libs.map(resolvePath): _*)
}

/** Finds scala jars of the version given, an in case of success builds shell commands for `scala` and `scalac`
 *  One cannot rely on [[scala.util.Properties.scalaCmd]] and [[scala.util.Properties.scalacCmd]],
 *    it's not guaranteed that the commands exist in PATH.
 *  Also no [[scala.util.Properties.scalaHome]] would be defined if SCALA_HOME environment variable is not set.
 */
trait ScalaResolver{
  def scala(ver: ScalaVersion): Try[List[String]]
  def scalac(ver: ScalaVersion): Try[List[String]]
}

object ClassPathResolver extends ScopedState[ClassPathResolver](new impl.ClassPathResolverImpl)
object ScalaResolver extends ScopedState[ScalaResolver](new impl.ScalaResolverImpl(ClassPathResolver.get))