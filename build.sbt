name := "http4s-circe-example"

version := "0.3"

scalaVersion := "2.13.18"

val http4sVersion = "0.23.38"
val circeVersion  = "0.14.17"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.http4s"        %% "http4s-dsl"          % http4sVersion,
  "org.http4s"        %% "http4s-ember-server" % http4sVersion,
  "org.http4s"        %% "http4s-ember-client" % http4sVersion,
  "org.http4s"        %% "http4s-circe"        % http4sVersion,
  "org.http4s"        %% "http4s-scalatags"    % "0.25.4",
  "io.circe"          %% "circe-generic"       % circeVersion,
  "io.circe"          %% "circe-literal"       % circeVersion,
  "org.typelevel"     %% "cats-effect"         % "3.7.1",
  "org.typelevel"     %% "log4cats-slf4j"      % "2.8.0",
  "ch.qos.logback"     % "logback-classic"     % "1.6.6",
  "com.alejandrohdezma" %% "http4s-munit"      % "3.0.0" % Test
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
