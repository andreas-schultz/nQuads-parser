package de.as.nquadsparser.util

import java.io.{IOException, File}

/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 11/8/11
 * Time: 7:22 PM
 * To change this template use File | Settings | File Templates.
 */

object TemporaryFileCreator {
  var tempDir = {
    val homeDir = System.getProperty("user.home")
    val nQuadsDir = new File(homeDir, ".nquads")
    if (!nQuadsDir.exists())
      nQuadsDir.mkdirs()
    nQuadsDir
  }

  def createTemporaryFile(prefix: String, suffix: String, deleteOnExit: Boolean = true): File = {
    val tempFile = File.createTempFile(prefix, suffix, tempDir)
    if (deleteOnExit)
      tempFile.deleteOnExit()
    return tempFile
  }

  def createTemporaryDirectory(prefix: String, suffix: String, deleteOnExit: Boolean = true): File = {
    val tempFile = File.createTempFile(prefix, suffix, tempDir)
    tempFile.delete()
    tempFile.mkdir()
    if(deleteOnExit)
      tempFile.deleteOnExit()

    return tempFile
  }

  def deleteDirOnExit(dir: File) {
    dir.deleteOnExit()
    val files = dir.listFiles
    if (files != null) {
      for (file <- files)
        if (file.isDirectory())
          deleteDirOnExit(file)
        else
          file.deleteOnExit()
    }
  }

  def setNewTempDir(directory: File) {
    if (directory.isDirectory)
      tempDir = directory
    else
      throw new IOException("File " + directory.getAbsolutePath + " is no directory.")
  }
}