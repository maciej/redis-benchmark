import _root_.pl.project13.scala.sbt.JmhPlugin

organization := "me.maciejb.redisbench"

name := "redis-benchmark"
version := "1.0"
scalaVersion := "2.11.7"

val akkaVersion = "2.4.2-RC3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.github.etaty" %% "rediscala" % "1.6.0",
  "com.typesafe" % "config" % "1.3.0"
)

enablePlugins(JmhPlugin)
