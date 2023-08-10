import sbt.Keys._
import sbt._

// some of these libraries are used by our flink pipelines and require scala 2.12
lazy val scala213               = "2.13.8"


lazy val Versions = new {
  val finch = "0.32.1"
  val logback = "1.4.7"
  val netty = "4.1.91.Final"
  val scalacheck = "3.2.10.0"
  val scalatest = "3.2.10"
  val slf4j = "2.0.7"
  val twitter = "20.9.0"
}

ThisBuild / scalaVersion := scala213

lazy val sharedSettings: Seq[Def.Setting[_]] = Seq(
  organization                           := "com.hypervolt",

  // If we comment this out, we get java.lang.NoClassDefFoundError: Could not initialize class io.netty.handler.codec.http.HttpClientCodec$Encoder
  libraryDependencies ++= Seq(
    "org.slf4j"          % "slf4j-api"        % Versions.slf4j,
    "org.slf4j"          % "log4j-over-slf4j" % Versions.slf4j,
    "org.slf4j"          % "jul-to-slf4j"     % Versions.slf4j,
    "org.scalatest"     %% "scalatest"        % Versions.scalatest  % Test,
    "org.scalatestplus" %% "scalacheck-1-15"  % Versions.scalacheck % Test,
    "ch.qos.logback"     % "logback-classic"  % Versions.logback    % Test,
  ),

  javacOptions ++= Seq("-source", "11", "-target", "11"),
)

lazy val defaultSettings = Defaults.coreDefaultSettings ++ sharedSettings

lazy val hypervolt = (project in file("."))
  .settings(
    name                    := "hypervolt",
    moduleName              := "hypervolt",
//    ThisBuild / useCoursier := false,
    defaultSettings,
//    crossScalaVersions := Nil,
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
    ),
  )

lazy val hvDomain = (project in file("libs/hv-domain"))
  .settings(
    defaultSettings,
    sharedSettings,
    moduleName := "hv-domain",
    libraryDependencies ++= Seq(
      "com.twitter"        %% "util-core"        % Versions.twitter,
      "io.netty"            % "netty-buffer"     % Versions.netty,
      "com.github.finagle" %% "finchx-circe"     % Versions.finch,
      "com.twitter"        %% "finagle-http"     % Versions.twitter,

// Makes it fail
//      "io.netty" % "netty-all" % Versions.netty,

    ),
    // not sure why this needs to be specified "twice"
    scalaVersion := scala213,
  )
  // Makes it fail
  .dependsOn(lang)


