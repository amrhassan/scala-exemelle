package exemelle

import java.io.InputStream
import scala.concurrent.ExecutionContext

/** [[StreamParser]] constructors */
object StreamParser {

  /** Constructs a stateful [[StreamParser]] backed by an [[InputStream]] */
  def fromInputStream(inputStream: InputStream)(implicit ec: ExecutionContext): StreamParser =
    UnsafeStreamParser.streamParser(UnsafeStreamParser(inputStream))
}
