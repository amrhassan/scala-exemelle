package exemelle

import java.io.InputStream

/** [[StreamParser]] constructors */
object StreamParser {

  /** Constructs a stateful [[StreamParser]] backed by an [[InputStream]] */
  def fromInputStream(inputStream: InputStream): StreamParser =
    UnsafeStreamParser.streamParser(UnsafeStreamParser(inputStream))
}
