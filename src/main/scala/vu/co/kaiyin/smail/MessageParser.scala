package vu.co.kaiyin.smail

import java.io.File

import scala.io.Source

/**
  * Parsing message files with a state machine.
  * Example message file:
  * <pre>
  * [t]
  * xxx@yyy.com, aaa@bbb.org
  * [s]
  * Test subject
  * [b]
  * Test mail body.
  * Say something here.
  * [a]
  * /tmp/test1.pdf
  * /tmp/test2.pdf
  * </pre>
  *
  * @param file
  */
case class MessageParser(file: File) {
  private val lines = Source.fromFile(file).getLines()
  private var parser: (String) => Unit = parseTo
  private var msg: MessageInfo = MessageInfo.empty

  private val HeaderRegex = """^\[[tsbal]\]""".r
  def isHeader(s: String) = HeaderRegex.findFirstIn(s).nonEmpty

  /**
    * Parse subjects. Comma or semicolon separated.
    * @param line
    */
  private def parseTo(line: String): Unit = {
    line.trim.split("""[,;]""").map(_.trim).filter(_.nonEmpty).foreach(x => {
      msg.to = msg.to + x
    })
    checkAndContinue(parseTo)
  }

  /**
    * Parse subject. Multiple lines allowed.
    * @param line
    */
  private def parseSubj(line: String): Unit = {
    val lineTrimmed = line.trim
    if(!lineTrimmed.isEmpty) msg.subject = msg.subject + lineTrimmed + "\n"
    checkAndContinue(parseSubj)
  }

  /**
    * Parse mail body. Multiple lines allowed.
    * @param line
    */
  private def parseBody(line: String): Unit = {
    msg.body = msg.body + line + "\n"
    checkAndContinue(parseBody)
  }

  /**
    * Parse attachments.
    * One filename per line.
    * @param line
    */
  private def parseAttachments(line: String): Unit = {
    val lineTrimmed = line.trim
    if(!lineTrimmed.isEmpty) msg.attachments = msg.attachments + line.trim
    checkAndContinue(parseAttachments)
  }

  /**
    * Parse labels. Comma or semicolon separated.
    * @param line
    */
  private def parseLabels(line: String): Unit = {
    val lineTrimmed = line.trim
    if(!lineTrimmed.isEmpty) msg.labels = (lineTrimmed.split("""[,;]""").map(_.trim) ++ msg.labels).toSet
    checkAndContinue(parseLabels)
  }

  def checkAndContinue(func: String => Unit): Unit = {
    if (!lines.hasNext) return
    else {
      val line = lines.next()
      if (isHeader(line)) dispatch(line) else func(line)
    }
  }


  def dispatch(line: String): Unit = {
    line match {
      case x if x.startsWith("[t]") =>
        checkAndContinue(parseTo)
      case x if x.startsWith("[b]") =>
        checkAndContinue(parseBody)
      case x if x.startsWith("[s]") =>
        checkAndContinue(parseSubj)
      case x if x.startsWith("[a]") =>
        checkAndContinue(parseAttachments)
      case x if x.startsWith("[l]") =>
        checkAndContinue(parseLabels)
      case _ => throw new Exception("Message file is malformatted.")
    }
  }

  def parse: MessageInfoIm = {
    checkAndContinue(parseTo)
    MessageInfo.finalize(msg)
  }

}

object MessageParser extends App {
  val p = MessageParser(new File(
    classOf[MessageParser].getResource("/mail.txt").getFile
  ))
  val m = p.parse
  m.to.split("""[,;]""").map(_.trim).sorted.toList == "abcde".map(x => x + "@" + x + ".com").toList
  m.subject.trim == "Test subject"
  m.body.trim == """Test mail body.
                    |Say something here.""".stripMargin
  m.attachments == Set("/tmp/test1.pdf", "/tmp/test2.pdf")
}
