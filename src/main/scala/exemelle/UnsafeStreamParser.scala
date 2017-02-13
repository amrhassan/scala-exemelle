package exemelle

import java.io.{InputStream, StringWriter}
import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamException}
import scala.collection.JavaConverters._
import cats.implicits._
import org.codehaus.stax2.{XMLEventReader2, XMLInputFactory2}


/** An XML stream parser that iterates over the [[Elem]]s in the underlying [[InputStream]], so non of
  * the public API is safe.
  *
  * Implementation similar to an [[Iterator]] but with a peek buffer of size 1.
  */
case class UnsafeStreamParser private(private val reader: XMLEventReader2) {

  private var buffer = Option.empty[Elem]

  /** Possibly retrieves the next [[Elem]] in the XML stream and effectively advancing the underlying
    * [[InputStream]]
    *
    * Returns [[None]] when reaches the end of the stream
    */
  def getNext(): Either[StreamError, Option[Elem]] = this.synchronized {
    if (buffer.nonEmpty) {
      val buffered = buffer
      buffer = Option.empty
      Right(buffered)
    } else {
      if (!reader.hasNext)
        Right(None)
      else
        Either.catchOnly[XMLStreamException](Some(unsafeNextElement)) leftMap (t ⇒ StreamError(t.getMessage))
    }
  }

  /** Peeks at the next element
    *
    * Causes the underlying [[InputStream]] to advance by one element
    */
  def peek(): Either[StreamError, Option[Elem]] = this.synchronized {
    if (buffer.nonEmpty)
      Right(buffer)
    else {
      for {
        next ← getNext()
      } yield {
        next foreach (elem ⇒ buffer = Some(elem))
        next
      }
    }
  }

  private def unsafeNextElement: Elem = {
    val event = reader.nextEvent()
    event.getEventType match {
      case XMLStreamConstants.START_ELEMENT ⇒ startTag(event.asStartElement())
      case XMLStreamConstants.END_ELEMENT ⇒ endTag(event.asEndElement())
      case XMLStreamConstants.START_DOCUMENT ⇒ startDocument(event)
      case XMLStreamConstants.CHARACTERS ⇒ text(event)
      case XMLStreamConstants.COMMENT ⇒ comment(event)
      case _ ⇒ Other(event.getEventType, event.toString)
    }
  }

  private def comment(elem: XMLEvent): Comment =
    Comment(originalNodeString(elem))

  private def endTag(elem: EndElement): EndTag =
    EndTag(elem.getName.getLocalPart, originalNodeString(elem))

  private def startDocument(e: XMLEvent): StartDocument =
    StartDocument(originalNodeString(e))

  private def text(e: XMLEvent): Text =
    Text(originalNodeString(e))

  private def startTag(elem: StartElement): StartTag = {
    val startElement = elem.asStartElement()
    val attributes = startElement.getAttributes.asScala map (_.asInstanceOf[javax.xml.stream.events.Attribute])
    StartTag(
      startElement.getName.getLocalPart,
      attributes.toList map (attr ⇒ Attribute(attr.getName.getLocalPart, attr.getValue)),
      originalNodeString(elem)
    )
  }

  private def originalNodeString(event: XMLEvent): String = {
    val buf = new StringWriter()
    event.writeAsEncodedUnicode(buf)
    buf.getBuffer.toString
  }
}

object UnsafeStreamParser {

  def apply(in: InputStream): UnsafeStreamParser = {
    val factory = XMLInputFactory.newInstance().asInstanceOf[XMLInputFactory2]
    val reader = factory.createXMLEventReader(in).asInstanceOf[XMLEventReader2]
    UnsafeStreamParser(reader)
  }

  /** Constructs [[StreamParser]] backed by the given [[UnsafeStreamParser]] */
  def streamParser(parser: UnsafeStreamParser): StreamParser =
    new StreamParser {
      def apply[A](op: StreamOp[A]): Either[StreamError, A] = op match {
        case Next ⇒ parser.getNext()
        case Peek ⇒ parser.peek()
      }
    }
}
