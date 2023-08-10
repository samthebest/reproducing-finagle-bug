import sbt.Keys._
import sbt._

// scalafmt: { maxColumn = 120, align.preset = more }

// ThisBuild / version := {
//   val short = git.gitHeadCommit.value.map(_.take(8))
//   s"${short.getOrElse(System.currentTimeMillis.toString)}"
// }

// some of these libraries are used by our flink pipelines and require scala 2.12
lazy val scala212               = "2.12.16"
lazy val scala213               = "2.13.8"
lazy val supportedScalaVersions = List(scala213, scala212)


  lazy val Versions = new {
    val algebird = "0.13.7"
    val auth0JavaJwt = "3.10.1"
    val auth0JwksRsa = "0.11.0"
    val awssdk = "1.11.728" // matches kinesis adaptor
    val awssdk2 = "2.15.14" // 2.14.x series lacks waiter classes
    val bouncyCastle = "1.67"
    val caffeine = "3.0.2"
    val chimney = "0.7.5"
    val circe = "0.13.0"
    val config = "1.3.4"
    val dropwizard = "4.2.15"
    val elastic4s = "7.9.1"
    val finch = "0.32.1"
    val googleAPIVersion = "1.30.4"
    val jam = "0.1.0"
    val kinesisAdaptor = "1.5.1"
    val logback = "1.4.7"
    val netty = "4.1.91.Final"
    val nettyRouter = "2.2.0"
    val scalacheck = "3.2.10.0"
    val scalatest = "3.2.10"
    val scanamo = "1.0.0-M15"
    val scodec = "1.11.7"
    val shapeless = "2.3.3"
    val slf4j = "2.0.7"
    val squants = "1.6.0"
    val testContainers = "0.40.8"
    val twitter = "20.9.0"
    val typesafeConfig = "1.3.4"
    val util = "0.57.0"
    val lambda = "1.2.2"
  }



ThisBuild / scalaVersion := scala213

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full,
)

def dockerTestDependencies(scope: String): Seq[Def.Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    "com.dimafeng" %% "testcontainers-scala-scalatest"  % Versions.testContainers % scope,
    "com.dimafeng" %% "testcontainers-scala-localstack" % Versions.testContainers % scope,
    "com.dimafeng" %% "testcontainers-scala-mockserver" % Versions.testContainers % scope,

    // At this time of writing, latest is 5.15, _BUT_ the client transitively pulls jackson-databind,
    // which conflicts with some twitter dep that expects version 2.11 ...
    "org.mock-server"    % "mockserver-client-java" % "5.11.2" % scope,
    "org.testcontainers" % "consul"                 % "1.18.3" % scope,

    // needed for service registration (our homegrown client doesn't yet support it)
    "com.ecwid.consul" % "consul-api" % "1.4.5" % scope,
  ),
)

lazy val sharedSettings: Seq[Def.Setting[_]] = Seq(
  organization                           := "com.hypervolt",
  Test / fork                            := true,
  Compile / packageDoc / publishArtifact := false,
  // ThisBuild / scalacOptions ++=
  //   ProjectDefaults.scalacOptionsList ++
  //     sys.env
  //       .get("CI")
  //       .filter { _ => scalaVersion.value == scala213 }
  //       .map(_ => "-Werror")
  //       .toSeq,
  libraryDependencies ++= Seq(
    "org.slf4j"          % "slf4j-api"        % Versions.slf4j,
    "org.slf4j"          % "log4j-over-slf4j" % Versions.slf4j,
    "org.slf4j"          % "jul-to-slf4j"     % Versions.slf4j,
    "org.scalatest"     %% "scalatest"        % Versions.scalatest  % Test,
    "org.scalatestplus" %% "scalacheck-1-15"  % Versions.scalacheck % Test,
    "ch.qos.logback"     % "logback-classic"  % Versions.logback    % Test,
  ),
  javacOptions ++= Seq("-source", "11", "-target", "11"),
  Test / testOptions ++= Seq(Tests.Argument("-oF"), Tests.Argument("-oD")),
  Test / javaOptions ++= Seq(
    "-Xmx2G",
    "-Djava.net.preferIPv4Stack=true",
    "-XX:MetaspaceSize=512m",
    "-XX:MaxMetaspaceSize=1g",
  ),
)

lazy val defaultSettings = Defaults.coreDefaultSettings ++ sharedSettings

lazy val hypervolt = (project in file("."))
  .settings(
    name                    := "hypervolt",
    moduleName              := "hypervolt",
    ThisBuild / useCoursier := false,
    defaultSettings,
    crossScalaVersions := Nil,
  )
  .aggregate(
    lang,
    hvDomain,
  )

lazy val lang = (project in file("libs/utils/lang"))
  .settings(
    sharedSettings,
    moduleName := "lang",
    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finchx-core"  % Versions.finch,
      "io.circe"           %% "circe-core"   % Versions.circe,
      "io.circe"           %% "circe-parser" % Versions.circe,
      "org.typelevel"      %% "squants"      % Versions.squants,
    ),
  )

lazy val fpCompilerPlugins: Seq[Def.Setting[_]] = Seq(
  addCompilerPlugin("org.typelevel"  %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin("org.augustjune" %% "context-applied"    % "0.1.3"),
)

lazy val hvDomain = (project in file("libs/hv-domain"))
  .settings(
    defaultSettings,
    sharedSettings,
    // CustomMergeStrat.mergeStrat,
    // Compile / sourceGenerators += Def.task {
    //   val schemaDir      = sourceDirectory.value / "schema"
    //   val commitHash     = IO.read(schemaDir / "commit-hash").trim
    //   val imola          = schemaDir / "imola.halogen.json"
    //   val sanmarino      = schemaDir / "sanmarino.halogen.json"
    //   val cloud          = schemaDir / "cloud.halogen.json"
    //   val destDir        = (Compile / sourceManaged).value
    //   val imolaFiles     = HalogenCodegen.genInto("Imola", imola, commitHash, destDir)
    //   val sanmarinoFiles = HalogenCodegen.genInto("SanMarino", sanmarino, commitHash, destDir)
    //   val cloudFiles     = HalogenCodegen.genInto("Cloud", cloud, commitHash, destDir)
    //   imolaFiles ++ sanmarinoFiles ++ cloudFiles
    // }.taskValue,
    moduleName := "hv-domain",
    libraryDependencies ++= Seq(
      "com.twitter"        %% "util-core"        % Versions.twitter,
      "io.netty"            % "netty-buffer"     % Versions.netty,
      "com.lihaoyi"        %% "upickle"          % "1.6.0",
      "io.circe"           %% "circe-core"       % Versions.circe,
      "io.circe"           %% "circe-generic"    % Versions.circe,
      "io.circe"           %% "circe-parser"     % Versions.circe,
      "org.apache.commons"  % "commons-compress" % "1.21",
      "org.tukaani"         % "xz"               % "1.9",
      "com.outworkers"     %% "util-samplers"    % Versions.util % Test,
      "com.github.finagle" %% "finchx-circe"     % Versions.finch,
      "com.twitter"        %% "finagle-http"     % Versions.twitter,

// Makes it fail
//      "io.netty" % "netty-all" % Versions.netty,



// Doesn't fail adding all these
//      "com.github.finagle" %% "finchx-core" % Versions.finch,
//      "io.circe" %% "circe-core" % Versions.circe,
//      "io.circe" %% "circe-parser" % Versions.circe,
//      "org.typelevel" %% "squants" % Versions.squants,



// Doesn't impact
//      "com.twitter"           %% "finagle-netty4"        % Versions.twitter,
//      "io.netty"       % "netty-buffer" % Versions.netty,
//      "com.github.finagle" %% "finchx-core"  % Versions.finch,

      "io.shaka"           %% "naive-http"       % "126",
    ),
    crossScalaVersions := supportedScalaVersions,
    // not sure why this needs to be specified "twice"
    scalaVersion := scala213,
  )
  // Makes it fail
  .dependsOn(lang)
// Makes it fail
//  .dependsOn(lang % "compile->compile;test->test;")
//  .dependsOn(aws, bareEncoding, lang % "compile->compile;test->test;")

