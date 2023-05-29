ThisBuild / tlBaseVersion := "0.14"
ThisBuild / circeRootOfCodeCoverage := None
ThisBuild / startYear := Some(2016)
ThisBuild / scalafixScalaBinaryVersion := "2.12"
ThisBuild / tlFatalWarningsInCi := false //TODO: ... fix this someday
ThisBuild / githubWorkflowBuildMatrixFailFast := Some(false)

val Versions = new {
  val circe = "0.14.4"
  val discipline = "1.5.1"
  val scalaCheck = "1.17.0"
  val scalaTest = "3.2.16"
  val scalaTestPlus = "3.2.11.0"
  val snakeYaml = "2.0"
  val snakeYamlEngine = "2.6"
  val previousCirceYamls = Set("0.14.0", "0.14.1", "0.14.2")

  val scala212 = "2.12.17"
  val scala213 = "2.13.10"
  val scala3 = "3.2.1"

  val scalaVersions = Seq(scala212, scala213, scala3)
}

ThisBuild / scalaVersion := Versions.scala213
ThisBuild / crossScalaVersions := Versions.scalaVersions

val root = tlCrossRootProject.aggregate(
  `circe-yaml-common`,
  `circe-yaml`,
  `circe-yaml-v12`
)

lazy val `circe-yaml-common` = project
  .in(file("circe-yaml-common"))
  .settings(
    description := "Library for converting between SnakeYAML's AST (YAML 1.1) and circe's AST",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % Versions.circe
    ),
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.3").toMap
  )

lazy val `circe-yaml` = project
  .in(file("circe-yaml"))
  .dependsOn(`circe-yaml-common`)
  .settings(
    description := "Library for converting between SnakeYAML's AST (YAML 1.1) and circe's AST",
    libraryDependencies ++= Seq(
      "org.yaml" % "snakeyaml" % Versions.snakeYaml,
      "io.circe" %% "circe-jawn" % Versions.circe % Test,
      "io.circe" %% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % Versions.scalaTestPlus % Test
    )
  )

lazy val `circe-yaml-v12` = project
  .in(file("circe-yaml-v12"))
  .dependsOn(`circe-yaml-common`)
  .settings(
    description := "Library for converting between snakeyaml-engine's AST (YAML 1.2) and circe's AST",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-jawn" % Versions.circe % Test,
      "org.snakeyaml" % "snakeyaml-engine" % Versions.snakeYamlEngine,
      "io.circe" %% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % Versions.scalaTestPlus % Test
    ),
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.3").toMap
  )

ThisBuild / developers := List(
  Developer("jeremyrsmith", "Jeremy Smith", "jeremyrsmith@gmail.com", url("https://github.com/jeremyrsmith")),
  Developer("jeffmay", "Jeff May", "jeff.n.may@gmail.com", url("https://github.com/jeffmay")),
  Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown"))
)
