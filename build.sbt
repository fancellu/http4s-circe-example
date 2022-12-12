name := "http4s-circe-example"

version := "0.2"

val http4sVersion = "0.23.16"

scalaVersion := "2.13.10"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % "0.23.12",
  "org.http4s" %% "http4s-blaze-client" % "0.23.12",
  "org.http4s" %% "http4s-circe" % http4sVersion,
//  "org.http4s" %% "http4s-twirl" % http4sVersion,
  "io.circe" %% "circe-generic" % "0.14.3",
  "io.circe" %% "circe-literal" % "0.14.3",
  "org.typelevel" %% "log4cats-core"   % "2.5.0",
  "ch.qos.logback" % "logback-classic" % "1.2.2",
  "com.github.fancellu" % "openai-scala-models" % "v1.1.0.beta3",
  "org.typelevel" %% "cats-effect" % "3.4.2",
  "org.http4s" %% "http4s-scalatags" % "0.25.1"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

libraryDependencies += "com.alejandrohdezma" %% "http4s-munit" % "0.14.0" % Test

//enablePlugins(SbtTwirl)
