package vu.co.kaiyin.smail

import java.io.{File, FileOutputStream}
import java.nio.file.{AccessDeniedException, Files, Path}
import java.util.zip.ZipOutputStream

import eu.medsea.mimeutil.MimeUtil
import eu.medsea.mimeutil.detector.{ExtensionMimeDetector, MagicMimeMimeDetector, OpendesktopMimeDetector}
import org.apache.commons.io.FileUtils
import resource._

/**
  * Created by IDEA on 4/8/16.
  */
object FileUtil {
  MimeUtil.registerMimeDetector(classOf[MagicMimeMimeDetector].getName)
  MimeUtil.registerMimeDetector(classOf[ExtensionMimeDetector].getName)
  MimeUtil.registerMimeDetector(classOf[OpendesktopMimeDetector].getName)
  def copyToDir(src: File, dest: File): Unit = {
    if(src.isFile) {
      FileUtils.copyFileToDirectory(src, dest)
    } else {
      FileUtils.copyDirectoryToDirectory(src, dest)
    }
  }
  def getMime(f: File): String = {
    MimeUtil.getMostSpecificMimeType(MimeUtil.getMimeTypes(f)).toString
  }

  // like the unix tree command
  def tree(fileNames: StringBuffer, dir: Path): String = {
    try {
      for (stream <- managed(Files.newDirectoryStream(dir))){
        val it = stream.iterator()
        while(it.hasNext) {
          val path = it.next()
          if(path.toFile().isDirectory) {
            tree(fileNames, path)
          } else {
            fileNames.append(path.toAbsolutePath).append("\n")
          }
        }
      }
    } catch {
      case e: AccessDeniedException =>
    }
    fileNames.toString
  }

  def zip: Unit = {
    for(zos <- managed(new ZipOutputStream(new FileOutputStream(new File("/tmp/x.zip"))))) {
      ZipArchiveUtil.addOneEntryHelper(zos, new File("/tmp/refs"))
    }
  }

  def main(args: Array[String]) {
    zip
  }
}
