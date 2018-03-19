package vu.co.kaiyin.smail

import java.io.File

import org.scalatest.FlatSpec



/**
  * Created by IDEA on 4/11/16.
  */
class MessageParserSpec extends FlatSpec {

  "Mail file" should "be parsed" in {
    val p = MessageParser(new File(
      classOf[MessageParserSpec].getResource("/mail.txt").getFile
    ))
    val m = p.parse
    assert(m.to.split("""[,;]""").map(_.trim).sorted.toList == "abcde".map(x => x + "@" + x + ".com").toList)
    assert(m.subject.trim == "Test subject")
    assert(m.body.trim == """Test mail body.
                       |Say something here.""".stripMargin)
    assert(m.attachments == Seq("/tmp/test1.pdf", "/tmp/test2.pdf").toSet)
    assert(m.labels == (1 to 6).map("label" + _).toSet + "smail")
  }

  "Header regex" should "match" in {
    val HeaderRegex = """^\[[tsbal]\]""".r
    assert(HeaderRegex.findFirstIn("[t]").get == "[t]")
    assert(HeaderRegex.findFirstIn("[s]").get == "[s]")
    assert(HeaderRegex.findFirstIn("[b]").get == "[b]")
    assert(HeaderRegex.findFirstIn("[a]").get == "[a]")
    assert(HeaderRegex.findFirstIn("[l]").get == "[l]")
  }

}
