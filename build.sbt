name := "exemelle"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.8"
organization := "io.github.amrhassan"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "0.6.1",
  "org.typelevel" %% "cats-free" % "0.6.1"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")
