name := "circe-yaml"
organization := "io.circe"
description := "Library for converting between SnakeYAML's AST and circe's AST"
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0")

bintrayRepository := "maven"
bintrayVcsUrl := Some("https://github.com/jeremyrsmith/circe-yaml")

val Versions = new {
  val circe = "0.6.1"
  val discipline = "0.7.2"
  val scalaCheck = "0.13.4"
  val scalaTest = "3.0.0"
  val snakeYaml = "1.17"
}

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % Versions.circe,
  "io.circe" %% "circe-parser" % Versions.circe,
  "org.yaml" % "snakeyaml" % Versions.snakeYaml,
  "io.circe" %% "circe-testing" % Versions.circe % "test",
  "org.typelevel" %% "discipline" % Versions.discipline % "test",
  "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % "test",
  "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
)
