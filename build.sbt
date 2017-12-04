name := "exemelle"
scalaVersion := "2.12.4"
organization := "io.github.amrhassan"
crossScalaVersions := Seq("2.11.8", scalaVersion.value)
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseCrossBuild := true

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "0.9.0",
  "org.typelevel" %% "cats-free" % "0.9.0",
  "com.fasterxml.woodstox" % "woodstox-core" % "5.0.2",
  "org.specs2" %% "specs2-core" % "4.0.2" % "test"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

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

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.php"))
homepage := Some(url("https://amrhassan.github.io/scala-exemelle"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/amrhassan/scala-exemelle"),
    "scm:git@github.com:amrhassan/scala-exemelle.git"
  )
)
developers := List(
  Developer(id="amrhassan", name="Amr Hassan", email="amr.hassan@gmail.com", url=url("http://amrhassan.info"))
)