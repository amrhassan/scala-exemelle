# exemelle #
[![Build Status](https://travis-ci.org/amrhassan/scala-exemelle.svg?branch=master)](https://travis-ci.org/amrhassan/scala-exemelle)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/exemelle_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.amrhassan/exemelle_2.11)


Reasonable parser combinators for XML element streams

# Usage #
```sbt
resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies += "io.github.amrhassan" %% "exemelle" % "0.1.0RC1"
```

```scala
import exemelle.StreamAction._
import exemelle.StreamParser

// You can describe how you want to process the stream in terms of combinators found in
// the StreamAction object

// Perhaps you want to capture the XML elements numbered 11 to 15?
val take5After10 = for {
    _ <- drop(10)
    elems <- take(5)
  } yield elems
  
// or drop everything until you encounter the "author" starting tag then take 5 elem?
import cats.implicits._   // For the >> shorthand
val dropUtilAuthor = dropUntil(_.name == "author") >> take(5)

// Or the full tag named "book"?
val bookTag = findTagNamed("book")

// Or al the full tags named "book"?
val allBookTags = findAllTagsNamed("book")


// You need parser implementation to parse through your stream
import scala.concurrent.ExecutionContext.Implicits.global
val parser = StreamParser.fromInputStream(/* a java.io.InputStream */)

// Now you can run one of the actions you've constructed earlier
run(parser)(allBookTags) // runs it into a Future[StreamError Xor Vector[Tag]]
```
