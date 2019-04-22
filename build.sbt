name := "udemy-akka-serialization"

version := "0.1"

scalaVersion := "2.12.8"


lazy val akkaVersion = "2.5.21"
lazy val protobufVersion = "3.6.1"

libraryDependencies ++= Seq(

  ///////////////////////////////////////////////////
  // Akka core
  ///////////////////////////////////////////////////
  
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,

  ///////////////////////////////////////////////////
  // Serialization frameworks
  ///////////////////////////////////////////////////
  
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1",
  "com.sksamuel.avro4s" %% "avro4s-core" % "2.0.4",
  "com.google.protobuf" % "protobuf-java" % "3.6.1",
  "io.spray" %%  "spray-json" % "1.3.5",

  ///////////////////////////////////////////////////
  // Persistent stores support
  ///////////////////////////////////////////////////

  // local levelDB stores
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  // JDBC with PostgreSQL 
  "org.postgresql" % "postgresql" % "42.2.2",
  "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.4.0",

  // Cassandra
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.91",
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.91" % Test,
)