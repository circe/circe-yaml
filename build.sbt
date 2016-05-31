name := "circe-yaml"
organization := "io.github.jeremyrsmith"
description := "Library for converting between SnakeYAML's AST and circe's AST"
version := "0.1.0"
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.10.6", "2.11.8")

libraryDependencies ++= Seq(
  "io.circe" %% "circe-parser" % "0.5.0-M1",
  "org.yaml" % "snakeyaml" % "1.17",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

bintrayRepository := "maven"
bintrayVcsUrl := Some("https://github.com/jeremyrsmith/circe-yaml")