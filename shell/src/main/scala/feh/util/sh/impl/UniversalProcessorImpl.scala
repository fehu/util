package feh.util.sh.impl

import feh.util.sh._
import scala.util.matching.Regex
import feh.util.sh.exec.{LibInfo, Import}
import feh.util.Platform

/** Default source processor implementation
 *
 * @param processorByName
 */
class UniversalProcessorImpl(val processorByName: Map[String, SourceProcessor],
                             val configKey: String, 
                             importKeyword: String,
                             importByKey: PartialFunction[String, Import],
                             importsPredefined: Seq[Import],
                             dependencyKeyword: String,
                             predefinedDependencies: Seq[LibInfo])
  extends UniversalProcessorLibs
  with ProcessorConfigSourceParser 
  with ProcessorConfigSourceParserAllKey
{
  val configRegex: Regex = """(?<name>\S+)(?:\:(?<args>.*))?""".r
  def configSeparators = Array(';')
  def configArgsSeparators = Array(',')

  def allKey = "#all"

  lazy val dependenciesProcessors =
    new SourceImportsExtractorImpl(importKeyword, importByKey, importsPredefined) ::
    new SourceDependenciesExtractorImpl(dependencyKeyword, predefinedDependencies, replace.comment) :: Nil

}

object UniversalProcessorImpl{
  trait ShExecutor extends SourceShProcessor{
    def shExecInj(sh: String) = Some("_root_.feh.util.shell.Exec(\"\"\"" + sh + "\"\"\")")
  }

  def configKey = "#conf"

  def keyShLine = "#sh "
  def keyShStart = "#sh>"
  def keyShEnd = "<sh#"

  lazy val processors = Map(
    "sh-line" -> new SourceShLineProcessor(keyShLine) with ShExecutor,
    "sh-block" -> new SourceShBlockProcessor(keyShStart, keyShEnd) with ShExecutor,
    "shortcuts" -> new SourceRegexReplaceProcessor(SourceRegexReplaceProcessor.shortcuts)
  )

  def importKeyword = "#import"

  lazy val importKeys = Map(
    "file" -> SourceImportsExtractorImpl.fileUtils,
    "exec" -> SourceImportsExtractorImpl.execUtils
  )

  def importsPredef = SourceImportsExtractorImpl.utils :: Nil

  def dependencyKeyword = "#lib"

  def dependenciesPredef = Libs.scala.library(scalaVersion) :: Nil

  def apply(processors: Map[String, SourceProcessor] = processors,
            configKey: String = configKey,
            importKeyword: String = importKeyword,
            importKeys: Map[String, Import] = importKeys,
            importsPredef: List[Import] = importsPredef,
            dependencyKeyword: String = dependencyKeyword,
            dependenciesPredef: Seq[LibInfo] = dependenciesPredef) =
    new UniversalProcessorImpl(processors, configKey,
                               importKeyword, importKeys, importsPredef,
                               dependencyKeyword, dependenciesPredef)

  def scalaVersion = Platform.scalaVersion
}
