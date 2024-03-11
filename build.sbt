lazy val akkaVersion = "2.9.0-M2"
lazy val akkaHttpVersion = "10.6.0-M1"
//lazy val scalaTestVersion = "3.0.5"

scalaVersion    := "2.13.13"
name := "akka-shop"

libraryDependencies ++= Seq(
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  // testing
//  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
//  "org.scalatest" %% "scalatest" % scalaTestVersion

)