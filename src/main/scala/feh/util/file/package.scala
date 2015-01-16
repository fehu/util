package feh.util

package object file extends FileUtils{
  type File = java.io.File

  object implicits extends FileImplicits with PathImplicits
}
