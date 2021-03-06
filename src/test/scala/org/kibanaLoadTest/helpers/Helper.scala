package org.kibanaLoadTest.helpers

import java.io.{File, PrintWriter}
import java.net.{MalformedURLException, URL}
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.{Calendar, Date, TimeZone}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.JsonParser
import spray.json.JsonParser.ParsingException

import scala.io.Source

object Helper {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  val logger: Logger = LoggerFactory.getLogger("Helper")

  def getDate(fieldNumber: Int, daysShift: Int): String = {
    val c: Calendar = Calendar.getInstance
    c.add(fieldNumber, daysShift)
    val dtf = DateTimeFormatter
      .ofPattern(dateFormat)
      .withZone(ZoneId.systemDefault())
    dtf.format(c.getTime.toInstant)
  }

  def convertDateToUTC(timestamp: Long): String = {
    val sdf = new SimpleDateFormat(dateFormat)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(new Date(timestamp))
  }

  def readResourceConfigFile(configName: String): Config = {
    val is = getClass.getClassLoader.getResourceAsStream(configName)
    if (is == null) {
      throw new RuntimeException(s"Config file is not found: $configName")
    }
    val source = scala.io.Source.fromInputStream(is).mkString
    ConfigFactory.parseString(source)
  }

  def loadJsonString(filePath: String): String = {
    val url = getClass.getClassLoader.getResource(filePath)
    if (url == null) {
      throw new RuntimeException(s"File is not found: $filePath")
    }
    Source.fromURL(url).getLines().mkString
  }

  def getTargetPath: String =
    Paths.get("target").toAbsolutePath.normalize.toString

  def getLastReportPath: String = {
    val dir: File = new File(getTargetPath + File.separator + "gatling")
    if (!dir.exists()) {
      return null
    }
    val files: Array[File] = dir.listFiles
    files.toList
      .filter(file => file.isDirectory)
      .maxBy(file => file.lastModified())
      .getAbsolutePath
  }

  def getReportFolderPaths: List[String] = {
    val dir: File = new File(getTargetPath + File.separator + "gatling")
    if (!dir.exists()) {
      return List.empty
    }
    val files: Array[File] = dir.listFiles
    files.toList
      .filter(file => file.isDirectory)
      .map(file => file.getAbsolutePath)
  }

  def validateUrl(
      str: String,
      errorMsg: String,
      checkPort: Boolean = true
  ): String = {
    try {
      val url = new URL(str)
      url.toURI
      if (checkPort) {
        url.getPort
      }
      str.replaceAll("/$", "")
    } catch {
      case ex: MalformedURLException =>
        throw new RuntimeException(s"$errorMsg\n ${ex.getMessage}")
      case ex: Exception =>
        throw new RuntimeException(s"Unknown error ${ex.getMessage}")
    }
  }

  def writeMapToFile(data: Map[String, Any], filePath: String): Unit = {
    new PrintWriter(filePath) {
      data.foreach {
        case (k, v) =>
          write(k + "=" + v)
          write("\n")
      }
      close()
    }
  }

  def readFileToMap(filePath: String): Map[String, Any] = {
    val lines: Iterator[String] =
      Source.fromFile(filePath).getLines().filter(str => str.trim.nonEmpty)
    lines
      .map(str =>
        (
          str.split("=", 2)(0),
          if (str.split("=", 2).length > 1) str.split("=", 2)(1) else ""
        )
      )
      .toMap
  }

  def getCIMeta: Map[String, String] = {
    Map(
      "buildId" -> Option(System.getenv("BUILD_ID")).getOrElse(""),
      "buildUrl" -> Option(System.getenv("BUILD_URL")).getOrElse(""),
      "kibanaBranchName" -> Option(System.getenv("KIBANA_BRANCH"))
        .getOrElse(""),
      "branchName" -> Option(System.getenv("branch_specifier")).getOrElse("")
    )
  }

  def isValidJson(str: String): Boolean = {
    try {
      JsonParser(str)
      true
    } catch {
      case e: ParsingException =>
        logger.error(s"isValidJson: Failed to parse string ${e.getMessage}")
        false
    }
  }
}
