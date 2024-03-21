lazy val akkaVersion = "2.5.23"
lazy val akkaHttpVersion = "10.1.8"
lazy val cassandraVersion = "0.98"

scalaVersion    := "2.13.13"
name := "akka-shop"

libraryDependencies ++= Seq(
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  // akka persistence
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  // Cassandra
  "com.typesafe.akka" %% "akka-persistence-cassandra" % cassandraVersion,
  // tests
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.2.15"
)