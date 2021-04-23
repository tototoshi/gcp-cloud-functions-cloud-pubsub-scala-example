val scalaVersion_2_13 = "2.13.5"

ThisBuild / organization := "com.github.tototoshi"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := scalaVersion_2_13

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "com.google.cloud.functions" % "functions-framework-api" % "1.0.4" % "provided",
    "com.google.cloud" % "google-cloud-pubsub" % "1.112.0",
    "com.google.cloud" % "google-cloudevent-types" % "0.3.0",
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalatest" %% "scalatest" % "3.2.8" % "test"
  )
)

lazy val function = project
  .in(file("function"))
  .settings(
    commonSettings,
    name := "function",
    assembly / assemblyOutputPath := baseDirectory.value / "dist" / "google-cloud-functions-scala-hello.jar"
  )

lazy val client = project
  .in(file("client"))
  .settings(
    commonSettings,
    name := "client"
  )

lazy val root = project
  .in(file("."))
  .aggregate(function, client)
