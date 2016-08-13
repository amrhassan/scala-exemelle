name := "exemelle"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.8"
organization := "io.github.amrhassan"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "0.6.1",
  "org.typelevel" %% "cats-free" % "0.6.1",
  "com.fasterxml.woodstox" % "woodstox-core" % "5.0.2",
  "org.specs2" %% "specs2-core" % "3.8.4" % "test",
  "org.specs2" %% "specs2-cats" % "3.8.4" % "test"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
  "-Xlint"
)

scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppresses problems with Scaladoc
)

pomExtra := (
  <url>https://amrhassan.github.io/scala-exemelle</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:amrhassan/scala-exemelle.git</url>
      <connection>scm:git:git@github.com:amrhassan/scala-exemelle.git</connection>
    </scm>
    <developers>
      <developer>
        <id>amrhassan</id>
        <name>Amr Hassan</name>
        <url>http://amrhassan.info</url>
      </developer>
    </developers>)
