organization in ThisBuild := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

val Versions = new {
  val circe = "0.9.0-M2"
  val discipline = "0.7.3"
  val scalaCheck = "1.13.5"
  val scalaTest = "3.0.3"
  val snakeYaml = "1.18"
  val previousCirceYaml = "0.6.1"
}

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

val root = project.in(file("."))
  .enablePlugins(GhpagesPlugin)
  .settings(
    name := "circe-yaml",
    description := "Library for converting between SnakeYAML's AST and circe's AST",
    scalacOptions ++= compilerOptions ++ (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, minor)) if minor >= 11 => Seq("-Ywarn-unused-import")
        case _ => Nil
      }
    ),
    scalacOptions in (Compile, console) ~= {
      _.filterNot(Set("-Ywarn-unused-import"))
    },
    scalacOptions in (Test, console) ~= {
      _.filterNot(Set("-Ywarn-unused-import"))
    },
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % Versions.circe,
      "io.circe" %% "circe-parser" % Versions.circe,
      "org.yaml" % "snakeyaml" % Versions.snakeYaml,
      "io.circe" %% "circe-testing" % Versions.circe % "test",
      "org.typelevel" %% "discipline" % Versions.discipline % "test",
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % "test",
      "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
    ),
    mimaPreviousArtifacts := Set("io.circe" %% "circe-yaml" % Versions.previousCirceYaml)
  )
  .settings(publishSettings ++ docSettings)

lazy val docSettings = Seq(
  autoAPIMappings := true,
  apiURL := Some(url("https://circe.github.io/circe-yaml/api/")),
  git.remoteRepo := "git@github.com:circe/circe-yaml.git",
  docMappingsApiDir := "api",
  addMappingsToSiteDir(mappings in (Compile, packageDoc), docMappingsApiDir),
  ghpagesNoJekyll := true,
  scalacOptions in (Compile, doc) ++= Seq(
    "-groups",
    "-implicits",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
   "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  )
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/circe/circe-yaml")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
