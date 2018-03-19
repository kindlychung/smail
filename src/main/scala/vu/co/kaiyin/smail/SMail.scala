package vu.co.kaiyin.smail

import java.io.InputStreamReader
import java.util.{Calendar, Collections}
import javax.activation.{CommandMap, MailcapCommandMap}

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.{Gmail, GmailScopes}
import org.docopt.Docopt


object SMail {

  private val user = "me"

  private val APPLICATION_NAME = "Gmail API Java Quickstart"

  private val DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/data_store")

  private var DATA_STORE_FACTORY: FileDataStoreFactory = _

  private val JSON_FACTORY = JacksonFactory.getDefaultInstance

  private var HTTP_TRANSPORT: HttpTransport = _

  private val SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM)

  try {
    HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR)
  } catch {
    case t: Throwable => {
      t.printStackTrace()
      System.exit(1)
    }
  }

  def authorize(): Credential = {
    val in = classOf[MailUtil].getResourceAsStream("/client_secret.json")
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(DATA_STORE_FACTORY)
      .setAccessType("offline")
      .build()
    val credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
      .authorize("user")
    println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath)
    credential
  }

  def getGmailService(): Gmail = {
    val credential = authorize()
    new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
      .setApplicationName(APPLICATION_NAME)
      .build()
  }

  val service = getGmailService
  val userProfile = service.users().getProfile(user).execute()
  val userEmail = userProfile.getEmailAddress
  val mailUtil = MailUtil(user, service)

  def now = {
    val t = Calendar.getInstance()
    s"${t.get(Calendar.HOUR_OF_DAY)}-${t.get(Calendar.MINUTE)}-${t.get(Calendar.SECOND)}"
  }

  val doc =
    """Usage:
      |  smail send    (-t <to>)... [(-s <subj>)] [(-b <body>)] [(-a <attach>)...] [(-l <label>)...]
      |  smail send    -m <msgfile>
      |  smail draft   (-t <to>)... [-s <subj>] [-b <body>] [(-a <attach>)...] [(-l <label>)...]
      |  smail draft   -m <msgfile>
      |  smail insert  (-t <to>)... [(-s <subj>)] [(-b <body>)] [(-a <attach>)...] [(-l <label>)...]
      |  smail insert  -m <msgfile>
      |
      |Options:
      |  -t        Recipient address.
      |  -s        Email subject. Default to "No subject".
      |  -b        Email body. Default to empty string.
      |  -a        Attachemnt.
      |  -l        Label. Default to "smail".
      |  -m        Use a message file and specify the options in it.
      |
      |Details:
      |  The `-l` option is ignored for the commands `send` and `draft`.
      |  Example message file:
      |
      |[t]
      |a@a.com, b@b.com
      |c@c.com
      |d@d.com, e@e.com
      |
      |[s]
      |Test subject
      |
      |[b]
      |Test mail body.
      |Say something here.
      |
      |[a]
      |/tmp/test1.pdf
      |/tmp/test2.pdf
      |
      |[l]
      |label1, label2, label3
      |label4
      |label5, label6
      |
      |Headers:
      |
      |[t]: To recipients. Separated by comma, semicolon, or newline.
      |[s]: Subject. Can span over multiple lines.
      |[b]: Mail body. Can span over multiple lines.
      |[a]: Attachments. File paths. One file per line.
      |[t]: Labels. Separated by comma, semicolon, or newline.
    """.stripMargin

  var msgInfo: MessageInfoIm = _

  /**
    * Main function.
    *
    * Prepare to run examples:
    * <pre>
    * cd /tmp
    * echo f1 > f1.txt && echo f2 > f2.txt
    * mkdir refs && cd refs
    * echo x > x.txt && echo y > y.txt
    * mkdir inner && cd inner
    * echo x > x.txt && echo y > y.txt
    * </pre>
    * Some examples:
    * <pre>
    * smail insert -t kindlychung@gmail.com -t enjoyvictor1@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail insert -t kindlychung@gmail.com -t enjoyvictor1@gmail.com -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail insert -t kindlychung@gmail.com -t enjoyvictor1@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail draft -t kindlychung@gmail.com -t enjoyvictor1@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail draft -t kindlychung@gmail.com -t enjoyvictor1@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail send -t kindlychung@gmail.com -t enjoyvictor1@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail send -t kindlychung@gmail.com -t enjoyvictor1@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail insert -m /tmp/mail.txt
    * smail draft -m /tmp/mail.txt
    * smail send -m /tmp/mail.txt
    * </pre>
    *
    * @param args
    */
  def main(args: Array[String]) {
    val mc = CommandMap.getDefaultCommandMap.asInstanceOf[MailcapCommandMap]
    mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
    mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
    mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
    mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
    mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822")
    val args1 = Docopt(doc, args)
    val msgFile = args1.getBoolean("-m", false)
    if (msgFile) {
      val parser = MessageParser(new java.io.File(args1.getString("<msgfile>").get))
      msgInfo = parser.parse
    } else {
      msgInfo = MessageInfoIm(
        args1.getStrings("<to>").mkString(","),
        args1.getString("<subj>").getOrElse("No subject"),
        args1.getString("<body>").getOrElse(""),
        args1.getStrings("<attach>").toSet,
        args1.getStrings("<label>") match {
          case x if x.isEmpty => Set("smail")
          case y => y.toSet
        }
      )
    }
    val msg = mailUtil.createEmailWithAttachments(
      msgInfo.to,
      userEmail,
      msgInfo.subject,
      msgInfo.body,
      msgInfo.attachments.map(x => new java.io.File(x)).toList
    )
    if (args1.getBoolean("send", false)) {
      mailUtil.sendMessage(msg)
    } else if (args1.getBoolean("draft", false)) {
      mailUtil.createDraft(msg)
    } else if (args1.getBoolean("insert", false)) {
      mailUtil.insertMessage(msg, msgInfo.labels.toList)
    }


  }


}