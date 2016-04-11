package vu.co.kaiyin.smail

import java.io.{File, InputStreamReader}
import java.util
import java.util.{Calendar, Collections}

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.{Gmail, GmailScopes}
import com.google.api.services.gmail.model._
import org.docopt.Docopt
import vu.co.kaiyin.smail.{MessageInfoIm, MessageParser, MailUtil}

import scala.collection.JavaConversions._


object SMail {

  private val user = "me"

  private val APPLICATION_NAME = "Gmail API Java Quickstart"

  private val DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/gmail-java-quickstart.json")

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
      |  smail send (-t <to>)... -s <subj>   [-b <body>] [(-a <attach>)...] [(-l <label>)...]
      |  smail send -m <msgfile>
      |  smail draft (-t <to>)... -s <subj>  [-b <body>] [(-a <attach>)...] [(-l <label>)...]
      |  smail draft -m <msgfile>
      |  smail insert (-t <to>)... -s <subj> [-b <body>] [(-a <attach>)...] [(-l <label>)...]
      |  smail insert -m <msgfile>
      |
      |Options:
      |  -t Recipient address.
      |  -s Email subject.
      |  -b Email body.
      |  -a Attachemnt.
      |  -l Label.
      |  -m Use a message file and specify the options in it.
    """.stripMargin

  var msgInfo: MessageInfoIm = _

  // Here is how
  //
  //
  /**
    * Main function.
    * Some examples:
    * <pre>
    * smail insert -t goodlychung@gmail.com -t enjoyvic@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail insert -t goodlychung@gmail.com -t enjoyvic@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail draft -t goodlychung@gmail.com -t enjoyvic@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail draft -t goodlychung@gmail.com -t enjoyvic@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail send -t goodlychung@gmail.com -t enjoyvic@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail send -t goodlychung@gmail.com -t enjoyvic@gmail.com -s "test subj" -b "test body" -a /tmp/f1.txt -a /tmp/f2.txt -a /tmp/refs
    * smail insert -m /tmp/mail.txt
    * smail draft -m /tmp/mail.txt
    * smail send -m /tmp/mail.txt
    * </pre>
    *
    * @param args
    */
  def main(args: Array[String]) {
    val args1 = Docopt(doc, args)
    val msgFile = args1.getString("-m")
    if (msgFile.nonEmpty) {
      val parser = MessageParser(new java.io.File(msgFile.get))
      msgInfo = parser.parse
    } else {
      msgInfo = MessageInfoIm(
        args1.getStrings("-t").mkString(","),
        args1.getString("-s").getOrElse("No subject"),
        args1.getString("-b").getOrElse(""),
        args1.getStrings("-a").toSet,
        args1.getStrings("-l") match {
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