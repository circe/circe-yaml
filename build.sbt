name := "circe-yaml-root"
organization in ThisBuild := "io.github.jeremyrsmith"
description in ThisBuild := "Library for converting between SnakeYAML's AST and circe's AST"

val commonSettings = Seq(
  version := "0.3.0",
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
  bintrayVcsUrl := Some("https://github.com/jeremyrsmith/circe-yaml"),

  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
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
