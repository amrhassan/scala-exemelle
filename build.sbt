name := "exemelle"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.8"
organization := "io.github.amrhassan"
crossScalaVersions := Seq("2.11.8", "2.12.0")

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "0.8.1",
  "org.typelevel" %% "cats-free" % "0.8.1",
  "com.fasterxml.woodstox" % "woodstox-core" % "5.0.2",
  "org.specs2" %% "specs2-core" % "3.8.6" % "test"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
  "-Xlint",
  "-feature",
  "-language:postfixOps",
	"-deprecation"
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
      <connection>scm:git@github.com:amrhassan/scala-exemelle.git</connection>
    </scm>
    <developers>
      <developer>
        <id>amrhassan</id>
        <name>Amr Hassan</name>
        <url>http://amrhassan.info</url>
      </developer>
    </developers>)
