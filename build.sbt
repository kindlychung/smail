name := "smail"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= List(
  "com.google.api-client" % "google-api-client" % "1.21.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.21.0",
  "com.google.apis" % "google-api-services-gmail" % "v1-rev40-1.21.0"
)

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.11"

libraryDependencies += "com.madgag" % "scala-arm_2.11" % "1.3.4"

libraryDependencies += "com.sun.mail" % "javax.mail" % "1.5.5"


libraryDependencies += "org.apache.directory.studio" % "org.apache.commons.io" % "2.4"

libraryDependencies += "eu.medsea.mimeutil" % "mime-util" % "2.1.3"

libraryDependencies += "org.scalactic" %% "scalactic" % "2.2.6"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"






resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.kindlychung" % "docopt.scala" % "0.2.2"

// The main class name must be full (including package names)
mainClass in Compile := Some("vu.co.kaiyin.smail.SMail")

mainClass in assembly := Some("vu.co.kaiyin.smail.SMail")


// How to invoke the tool:
// java -jar /Users/kaiyin/IdeaProjects/scaladrive/target/scala-2.11/scaladrive-assembly-1.0.jar
// java -cp /Users/kaiyin/IdeaProjects/scaladrive/target/scala-2.11/scaladrive-assembly-1.0.jar vu.co.kaiyin.scaladrive.SDrive
