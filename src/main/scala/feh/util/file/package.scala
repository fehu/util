package feh.util

package object file extends FileUtilWrappers with PathImplicits{
  type File = java.io.File
  lazy val File = new FileUtils{}

//  object implicits extends
}
