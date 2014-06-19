package feh.util

object Platform extends RuntimePlatform with RuntimePlatforms{
  lazy val name = sys.props("os.name")
  
  lazy val check = new PlatformCheck{
    def isLinux = name.toLowerCase contains "linux"
    def isOsX = name.toLowerCase |> { nme => nme.contains("osx") || nme.contains("os x") }
    def isWindows = name.toLowerCase contains "windows"
  }

  protected lazy val platformMapping = Map(
    ((_: PlatformCheck).isLinux) -> Linux,
    ((_: PlatformCheck).isOsX) -> OsX,
    ((_: PlatformCheck).isWindows) -> Windows
  )
  protected def unknownPlatform = sys.error(s"unsupported platform $name")
}

trait RuntimePlatform{
  def name: String
  def check: PlatformCheck

  protected def platformMapping: Map[PlatformCheck => Boolean, Platform]
  protected def unknownPlatform: Nothing

  lazy val platform = platformMapping.find(_._1(check)).getOrElse(unknownPlatform)._2
  
  trait Platform

  trait PlatformCheck{
    def isLinux: Boolean
    def isOsX: Boolean
    def isWindows: Boolean

    def isUnix = isLinux || isOsX
  }

  def assert(f: PlatformCheck => Boolean, msg: String = null): Unit = if(!f(check))
    throw unsupported(msg)
  def assert(f: Platform => Option[String]): Unit = f(platform) foreach(msg => throw unsupported(msg))
  def unsupported(msg: String = null) = PlatformException(name, Option(msg))

  case class PlatformException(platformName: String, msg: Option[String]) extends Exception(
    s"PlatformException(${msg.getOrElse(platformName + " is not supported")}})"
  )

}

trait RuntimePlatforms{
  self: RuntimePlatform =>

  trait Unix extends Platform

  case object Linux extends Unix
  case object OsX extends Unix
  case object Windows extends Platform
}
