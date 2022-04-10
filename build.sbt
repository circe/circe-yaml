ThisBuild / organization := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)

val Versions = new {
  val circe = "0.14.1"
  val discipline = "1.5.0"
  val scalaCheck = "1.15.4"
  val scalaTest = "3.2.11"
  val scalaTestPlus = "3.2.11.0"
  val snakeYaml = "1.28"
  val previousCirceYaml = "0.13.1"
}

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.7", "3.0.2")

val root = project
  .in(file("."))
  .enablePlugins(GhpagesPlugin)
  .settings(
    name := "circe-yaml",
    description := "Library for converting between SnakeYAML's AST and circe's AST",
    scalacOptions ++= compilerOptions,
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(
            "-Xfuture",
            "-Yno-adapted-args",
            "-Ywarn-unused-import"
          )
        case _ =>
          Seq(
            "-Ywarn-unused:imports"
          )
      }
    },
    Compile / console / scalacOptions ~= {
      _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
    },
    Test / console / scalacOptions ~= {
      _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
    },
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % Versions.circe,
      "io.circe" %% "circe-jawn" % Versions.circe % Test,
      "org.yaml" % "snakeyaml" % Versions.snakeYaml,
      "io.circe" %% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % Versions.scalaTestPlus % Test
    ),
    mimaPreviousArtifacts := Set("io.circe" %% "circe-yaml" % Versions.previousCirceYaml)
  )
  .settings(publishSettings ++ docSettings)

lazy val docSettings = Seq(
  autoAPIMappings := true,
  apiURL := Some(url("https://circe.github.io/circe-yaml/api/")),
  git.remoteRepo := "git@github.com:circe/circe-yaml.git",
  docMappingsApiDir := "api",
  addMappingsToSiteDir(Compile / packageDoc / mappings, docMappingsApiDir),
  ghpagesNoJekyll := true,
  Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-implicits",
    "-doc-source-url",
    scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath
  )
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  homepage := Some(url("https://github.com/circe/circe-yaml")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/circe/circe-yaml"),
      "scm:git:git@github.com:circe/circe-yaml.git"
    )
  ),
  developers := List(
    Developer("jeremyrsmith", "Jeremy Smith", "jeremyrsmith@gmail.com", url("https://github.com/jeremyrsmith")),
    Developer("jeffmay", "Jeff May", "jeff.n.may@gmail.com", url("https://github.com/jeffmay")),
    Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown"))
  )
)

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
// No auto-publish atm. Remove this line to generate publish stage
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("clean", "coverage", "test", "coverageReport", "scalastyle", "scalafmtCheckAll"),
    id = None,
    name = Some("Test"),
    cond = Some("matrix.scala != '3.0.2'")
  ),
  WorkflowStep.Sbt(
    List("clean", "test"),
    id = None,
    name = Some("Test"),
    cond = Some("matrix.scala == '3.0.2'")
  ),
  WorkflowStep.Use(
    UseRef.Public(
      "codecov",
      "codecov-action",
      "v1"
    ),
    cond = Some("matrix.scala != '3.0.2'")
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
