package vu.co.kaiyin.smail

/**
  * Parsed result of a message file.
  *
  * @param to
  * @param subject
  * @param body
  * @param attachments
  */
case class MessageInfo(var to: Set[String] = Set.empty,
                       var subject: String = "",
                       var body: String = "",
                       var attachments: Set[String] = Set.empty,
                       var labels: Set[String] = Set("smail"))

object MessageInfo {

  def empty = MessageInfo()

  def finalize(msg: MessageInfo) = {
    MessageInfoIm(
      msg.to.mkString(","),
      msg.subject,
      msg.body,
      msg.attachments,
      msg.labels
    )
  }
}

case class MessageInfoIm(val to: String, val subject: String, val body: String, val attachments: Set[String] = Set.empty, val labels: Set[String] = Set("smail"))
