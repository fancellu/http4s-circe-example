name := "http4s-circe-example"

version := "0.2"

val http4sVersion = "0.23.16"

scalaVersion := "2.13.10"

// resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % "0.23.12",
  "org.http4s" %% "http4s-blaze-client" % "0.23.12",
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-twirl" % http4sVersion,
  "io.circe" %% "circe-generic" % "0.14.3",
  "io.circe" %% "circe-literal" % "0.14.3",
  "org.typelevel" %% "log4cats-slf4j"   % "2.5.0",
  "org.slf4j" % "slf4j-simple" % "1.7.36",
)

libraryDependencies += "com.alejandrohdezma" %% "http4s-munit" % "0.14.0" % Test

enablePlugins(SbtTwirl)
