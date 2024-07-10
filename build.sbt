ThisBuild / tlBaseVersion := "0.15"
ThisBuild / circeRootOfCodeCoverage := None
ThisBuild / startYear := Some(2016)
ThisBuild / tlFatalWarnings := false //TODO: ... fix this someday
ThisBuild / githubWorkflowBuildMatrixFailFast := Some(false)

val Versions = new {
  val circe = "0.14.9"
  val discipline = "1.7.0"
  val scalaCheck = "1.18.0"
  val scalaTest = "3.2.18"
  val scalaTestPlus = "3.2.18.0"
  val snakeYaml = "2.2"
  val snakeYamlEngine = "2.7"
  val previousCirceYamls = Set("0.14.0", "0.14.1", "0.14.2")

  val scala213 = "2.13.14"
  val scala3 = "3.3.3"

  val scalaVersions = Seq(scala213, scala3)
}

ThisBuild / scalaVersion := Versions.scala213
ThisBuild / crossScalaVersions := Versions.scalaVersions

val root = tlCrossRootProject.aggregate(
  `circe-yaml-common`,
  `circe-yaml`,
  `circe-yaml-v12`,
  `circe-yaml-scalayaml`
)

lazy val `circe-yaml-common` = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("circe-yaml-common"))
  .settings(
    description := "Library for converting between SnakeYAML's AST (YAML 2.0) and circe's AST",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % Versions.circe
    ),
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.15.2").toMap
  )

lazy val `circe-yaml` = project
  .in(file("circe-yaml"))
  .dependsOn(`circe-yaml-common`.jvm)
  .settings(
    description := "Library for converting between SnakeYAML's AST (YAML 2.0) and circe's AST",
    libraryDependencies ++= Seq(
      "org.yaml" % "snakeyaml" % Versions.snakeYaml,
      "io.circe" %% "circe-jawn" % Versions.circe % Test,
      "io.circe" %% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % Versions.scalaTestPlus % Test
    )
  )

lazy val `circe-yaml-v12` = project
  .in(file("circe-yaml-v12"))
  .dependsOn(`circe-yaml-common`.jvm)
  .settings(
    description := "Library for converting between snakeyaml-engine's AST (YAML 2.0) and circe's AST",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-jawn" % Versions.circe % Test,
      "org.snakeyaml" % "snakeyaml-engine" % Versions.snakeYamlEngine,
      "io.circe" %% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % Versions.scalaTestPlus % Test
    ),
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.3").toMap
  )

lazy val `circe-yaml-scalayaml` = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .dependsOn(`circe-yaml-common`)
  .settings(
    description := "Library for converting between scala-yaml AST and circe's AST",
    libraryDependencies ++= Seq(
      "org.virtuslab" %%% "scala-yaml" % "0.1.0",
      "org.scalatest" %%% "scalatest" % Versions.scalaTest % Test
    ),
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.15.2").toMap
  )

ThisBuild / developers := List(
  Developer("jeremyrsmith", "Jeremy Smith", "jeremyrsmith@gmail.com", url("https://github.com/jeremyrsmith")),
  Developer("jeffmay", "Jeff May", "jeff.n.may@gmail.com", url("https://github.com/jeffmay")),
  Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown"))
)
