name := "circe-yaml"
version in ThisBuild := "0.3.0"
organization in ThisBuild := "io.github.jeremyrsmith"
description in ThisBuild := "Library for converting between SnakeYAML's AST and circe's AST"
licenses in ThisBuild += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

val Versions = new {
  val circe = "0.6.1"
  val discipline = "0.7.2"
  val scalaCheck = "0.13.4"
  val scalaTest = "3.0.0"
  val snakeYaml = "1.17"
}

val Libraries = new {
  val circeCore = "io.circe" %% "circe-core" % Versions.circe
  val circeParser = "io.circe" %% "circe-parser" % Versions.circe
  val circeTesting = "io.circe" %% "circe-testing" % Versions.circe
  val discipline = "org.typelevel" %% "discipline" % Versions.discipline
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Versions.scalaCheck
  val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest
  val snakeYaml = "org.yaml" % "snakeyaml" % Versions.snakeYaml
}

val commonSettings = Seq(
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8"),

  libraryDependencies ++= Seq(
    Libraries.circeCore
  ) ++ Seq(
    // Test-only dependencies
    Libraries.circeTesting,
    Libraries.discipline,
    Libraries.scalaCheck,
    Libraries.scalaTest
  ).map(_ % Test),

  bintrayRepository := "maven",
  bintrayVcsUrl := Some("https://github.com/jeremyrsmith/circe-yaml")
)

lazy val snake = (project in file("snake"))
  .settings(commonSettings)
  .settings(
    name := "circe-yaml-snake",
    libraryDependencies ++= Seq(
      Libraries.circeParser,
      Libraries.snakeYaml
    )
  )
  .dependsOn(testing % Test)

lazy val testing = (project in file("testing"))
  .settings(commonSettings)
  .settings(
    name := "circe-yaml-testing",
    libraryDependencies ++= Seq(
      Libraries.snakeYaml,
      Libraries.circeTesting,
      Libraries.discipline
    )
  )

lazy val `circe-yaml` = (project in file (".")).settings(
  commonSettings
) dependsOn (snake) //aggregate (snake)
