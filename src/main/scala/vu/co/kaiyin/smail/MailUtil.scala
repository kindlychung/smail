package vu.co.kaiyin.smail

import java.io.{ByteArrayOutputStream, File}
import java.util
import java.util.Properties
import javax.activation.{DataHandler, FileDataSource}
import javax.mail.Session
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.{Draft, Label, Message, ModifyMessageRequest}
import vu.co.kaiyin.smail.ZipArchiveUtil.addToTgz

import scala.collection.JavaConversions._

//remove if not needed

case class MailUtil(val user: String, val service: Gmail) {

  def getLabels: util.List[Label] = {
    val listResponse = service.users().labels().list(user).execute()
    val labels = listResponse.getLabels
    //    val x = labels.get(0).getId
    labels
  }

  def createDraft(email: MimeMessage): Draft = {
    val message = createMessageWithEmail(email)
    var draft = new Draft()
    draft.setMessage(message)
    draft = service.users().drafts().create(user, draft).execute()
    println("draft id: " + draft.getId)
    println(draft.toPrettyString())
    draft
  }

  def createMessageWithEmail(email: MimeMessage): Message = {
    val bytes = new ByteArrayOutputStream()
    email.writeTo(bytes)
    val encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray())
    val message = new Message()
    message.setRaw(encodedEmail)
    //    message.setLabelIds(List("a", "b"))
    message
  }

  def createEmail(to: String,
                  from: String,
                  subject: String,
                  bodyText: String): MimeMessage = {
    val props = new Properties()
    val session = Session.getDefaultInstance(props, null)
    val email = new MimeMessage(session)
    email.setFrom(new InternetAddress(from))
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to))
    email.setSubject(subject)
    email.setText(bodyText)
    email
  }

  def sendDraft(draftId: String) {
    val draft = new Draft()
    draft.setId(draftId)
    val message = service.users().drafts().send(user, draft).execute()
    println("Draft with ID: " + draftId + " sent successfully.")
    println("Draft sent as Message with ID: " + message.getId)
  }

  /**
    * Create email with attachments
    * @param to Comma separated list of email addresses
    * @param from
    * @param subject
    * @param bodyText
    * @param files
    * @return
    */
  def createEmailWithAttachments(to: String,
                                 from: String,
                                 subject: String,
                                 bodyText: String,
                                 files: Seq[File]): MimeMessage = {
    val props = new Properties()
    val session = Session.getDefaultInstance(props, null)
    val email = new MimeMessage(session)
    val fAddress = new InternetAddress(from)
    email.setFrom(fAddress)
    email.addRecipients(javax.mail.Message.RecipientType.TO, to)
    email.setSubject(subject)
    val mimeBodyPart = new MimeBodyPart()
    mimeBodyPart.setContent(bodyText, "text/plain")
    mimeBodyPart.setHeader("Content-Type", "text/plain; charset=\"UTF-8\"")
    val multipart = new MimeMultipart()
    multipart.addBodyPart(mimeBodyPart)

    def addFileAsPart(file: File): Unit = {
      if (file.isDirectory) {
        addFileAsPart(addToTgz(file))
      } else {
        val mimeBodyPart = new MimeBodyPart()
        val source = new FileDataSource(file)
        mimeBodyPart.setDataHandler(new DataHandler(source))
        mimeBodyPart.setFileName(file.getName)
        val contentType = FileUtil.getMime(file)
        mimeBodyPart.setHeader("Content-Type", contentType + "; name=\"" + file.getName + "\"")
        mimeBodyPart.setHeader("Content-Transfer-Encoding", "base64")
        multipart.addBodyPart(mimeBodyPart)
      }
    }
    files.foreach(addFileAsPart(_))

    email.setContent(multipart)
    email
  }

  def addLabelsToMessage(labelList: Seq[String], representation: LabelRepresentation, msg: Message): Message = {
    def modifyMsg(labelsToAdd: Seq[String]) = {
      val modRequest = new ModifyMessageRequest().setAddLabelIds(labelsToAdd)
      service.users().messages().modify(user, msg.getId, modRequest).execute()
    }
    val labels = getLabels
    val labelIds = labels.map(_.getId)
    val labelNames = labels.map(_.getName)
    representation match {
      case LabelRepresentation.ID =>
        val parts = labelList.partition(labelIds.contains(_))
        if (parts._2.length > 0) {
          throw new Exception("Some label IDs don't exist.")
        }
        modifyMsg(parts._1)
      case LabelRepresentation.NAME =>
        val parts = labelList.partition(labelNames.contains(_))
        val newLabels = parts._2.map(creatLabel(_, checkExist = false).getId)
        val oldLabels = labels.filter(x => parts._1.contains(x.getName)).map(_.getId)
        modifyMsg(oldLabels ++ newLabels)
    }
  }

  def creatLabel(labelName: String, labelListVisibility: String = "labelShow", messageListVisibility: String = "show", checkExist: Boolean = true): Label = {
    def create = {
      val label = new Label().setName(labelName)
        .setLabelListVisibility(labelListVisibility)
        .setMessageListVisibility(messageListVisibility)
      service.users().labels().create(user, label).execute()
      label
    }
    val labels = getLabels
    if (checkExist) labels.filter(_.getName == labelName).headOption.getOrElse({
      create
    })
    else create
  }

  /**
    * Insert a message in the mailbox, similar to sendMessage, but has the ability to add labels
    *
    * @see sendMessage
    * @param email
    * @param labelList
    * @param representation
    * @return
    */
  def insertMessage(email: MimeMessage, labelList: Seq[String] = Seq.empty, representation: LabelRepresentation = LabelRepresentation.NAME): Message = {
    val message: Message = addLabels(createMessageWithEmail(email), labelList, representation)
    service.users().messages().insert(user, message).execute()
  }

  def addLabels(message: Message, labelList: Seq[String], representation: LabelRepresentation): Message = {
    if (labelList.isEmpty) {
      return message
    }
    val labels = getLabels
    val labelIds = labels.map(_.getId)
    val labelNames = labels.map(_.getName)
    representation match {
      case LabelRepresentation.ID =>
        val parts = labelList.partition(labelIds.contains(_))
        if (parts._2.length > 0) {
          throw new Exception("Some label IDs don't exist.")
        }
        message.setLabelIds(parts._1)
      case LabelRepresentation.NAME =>
        val parts = labelList.partition(labelNames.contains(_))
        val newLabels = parts._2.map(creatLabel(_, checkExist = false).getId)
        val oldLabels = labels.filter(x => parts._1.contains(x.getName)).map(_.getId)
        message.setLabelIds(newLabels ++ oldLabels)
    }
    message
  }

  def sendMessage(email: MimeMessage) = {
    val message = createMessageWithEmail(email)
    service.users().messages().send(user, message).execute()
  }
}

