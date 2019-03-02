name := "broadwayctrl"
version := "1.0"

scalaVersion := "2.12.3"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

val akkaVersion = "2.5.4"
val akkaHttpVersion = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.megard" %% "akka-http-cors" % "0.2.1",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "org.rxtx" % "rxtx" % "2.1.7",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.1-akka-2.5.x",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7"  
)


libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-simple")) }

      
      
import sbt.Package.ManifestAttributes
packageOptions  := Seq(ManifestAttributes(("Main-Class", "com.gounder.mediaplayer.htd.rest.MediaPlayerService"), ("Built-By","sbt"), ("Implementation-Title", "HTD Media Server"), ("Implementation-Version", "1.0")))

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case "reference.conf" => MergeStrategy.concat
 case x => MergeStrategy.first
}