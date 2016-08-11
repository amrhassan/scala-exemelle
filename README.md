# exemelle #
Parser combinators for XML element streams

# Usage #
```sbt
resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies += "io.github.amrhassan" %% "exemelle" % "0.1.0-SNAPSHOT"
```

```scala
import exemelle.StreamJob._
import exemelle.StreamParser

// You can describe how you want to process the stream in terms of combinators found in
// the StreamJob object

// Perhaps you want to capture the XML elements numbered 11 to 15?
val take5After10 = drop(10) >> take(5)

// Or the full tag named "book"?
val bookTag = takeTagNamed("book")

// Or al the full tags named "book"?
val allBookTags = takeTagsNamed("book")


// You need parser implementation to parse through your stream
import scala.concurrent.ExecutionContext.Implicits.global
val parser = StreamParser.fromInputStream(/* a java.io.InputStream */)

// Now you can run one of the actions you've constructed earlier
run(parser)(allBookTags) // runs it into a Future[StreamError Xor Vector[Tag]]
```
