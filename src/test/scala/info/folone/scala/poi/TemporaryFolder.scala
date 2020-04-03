package info.folone.scala.poi

import java.io.File
import org.specs2.specification.AfterAll

trait TemporaryFolder extends AfterAll {
  override def afterAll: Unit = {
    Option(tempDir.listFiles).map(_.toList).getOrElse(Nil).foreach(_.delete)
    tempDir.delete
  }

  lazy val tempDir: File = {
    val dir = File.createTempFile("test", "")
    dir.delete
    dir.mkdir
    dir
  }

  lazy val tempDirPath: String = tempDir.getAbsolutePath

}
