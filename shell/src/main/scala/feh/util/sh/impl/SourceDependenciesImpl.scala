package feh.util.sh.impl

import feh.util.sh.{SourceImportsExtractor, SourceDependenciesExtractor, Libs, SourceImports}
import feh.util.sh.exec.{LibInfo, Import, DependencyInfo}
import feh.util.FileUtils._

class SourceImportsExtractorImpl(val importKey: String,
                        val importByKey: PartialFunction[String, Import],
                        val predefinedImports: Seq[Import])
  extends SourceImportsExtractor
{
  /** reads and removes import definitions from source */
  protected def extractImports(source: StringBuilder, params: Seq[String]) = Nil                                      // todo

  def toString(seq: Seq[DependencyInfo]) = imports(seq)
    .map{
      case (p, i :: Nil) => "import " + (p / i).path.mkString(".")
      case (p, is) => "import " + p.path.mkString(".") + "{" + is.mkString(", ") + "}"
    }
    .mkString("\n")

  protected def imports(inf: Seq[DependencyInfo]) = {
    val wild = inf.collect { case Import(path, true, _) => path }
    val imports = inf.collect { case Import(path, false, _) => path } |> Path.group
    imports ++ wild.map(_ -> Seq("_"))
  }

  protected def insert(imports: String, in: StringBuilder) = in.insert(0, imports)
}


object SourceImportsExtractorImpl{
  lazy val utils = Import("feh" / "util", wildcard = true, Seq(Libs.feh.util))
  lazy val fileUtils = utils.transform(_ / "FileUtils")
  lazy val execUtils = utils.transform(_ / "ExecUtils")
}


class SourceDependenciesExtractorImpl(val dependencyKey: String,
                                      val predefinedDependencies: Seq[LibInfo])
  extends SourceDependenciesExtractor
{
  /** reads and removes import definitions from source */
  protected def extractDependencies(source: StringBuilder, params: Seq[String]) = Nil                                 // todo
}