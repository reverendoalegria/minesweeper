name := "minesweeper"
 
version := "1.2"

organization := "romangarcia"
      
lazy val `minesweeper` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  ehcache,
  ws,
  guice,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

// ··· Docker Configuration ···
enablePlugins(JavaAppPackaging)

maintainer in Docker := "Roman Garcia"

dockerBaseImage := "openjdk:8-jre"

dockerRepository := Some("gcr.io/minesweeper-api-1")

dockerUpdateLatest := true

dockerExposedPorts := Seq(9000)

packageName in Docker := packageName.value
      