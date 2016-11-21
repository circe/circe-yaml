import sbt._

object Libraries {

  private val circeVersion = "0.6.0"
  private val disciplineVersion = "0.7.2"
  private val scalaCheckVersion = "0.13.4"
  private val scalaTestVersion = "3.0.0"
  private val snakeYamlVersion = "1.17"

  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val circeTesting = "io.circe" %% "circe-testing" % circeVersion
  val discipline = "org.typelevel" %% "discipline" % disciplineVersion
  val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
  val snakeYaml = "org.yaml" % "snakeyaml" % snakeYamlVersion
}
