package exemelle

import java.io.{InputStream, StringWriter}
import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamException}
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Xor, XorT}
import cats.implicits._
import org.codehaus.stax2.{XMLEventReader2, XMLInputFactory2}


/** An XML stream parser that changes the state of its underlying InputStream */
case class UnsafeStreamParser private(private val reader: XMLEventReader2) {

  private val buffer = StreamBuffer.empty[Elem]

  /** Possibly retrieves the next element in the XML stream and effectively advancing the underlying
    * InputStream
    */
  def getNext: StreamError Xor Option[Elem] = this.synchronized {
    if (buffer.nonEmpty)
      buffer.get.right
    else {
      if (!reader.hasNext)
        None.right
      else
        Xor.catchOnly[XMLStreamException](Some(unsafeNextElement)) leftMap (t ⇒ StreamError(t.getMessage))
    }
  }

  /** Peeks at the next element
    *
    * Causes the underlying InputStream to advance
    */
  def peek: StreamError Xor Option[Elem] = this.synchronized {
    if (buffer.nonEmpty)
      buffer.peek.right
    else {
      for {
        next ← getNext
      } yield {
        next foreach buffer.add
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
    val attributes = startElement.getAttributes map (_.asInstanceOf[javax.xml.stream.events.Attribute])
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
  def streamParser(parser: UnsafeStreamParser)(implicit ec: ExecutionContext): StreamParser = new StreamParser {
    def apply[A](op: StreamOp[A]): XorT[Future, StreamError, A] = op match {
      case Next ⇒ XorT(Future(parser.getNext))
      case Peek ⇒ XorT(Future(parser.peek))
    }
  }
}

private [exemelle] class StreamBuffer[A] {

  private var buff = List.empty[A]

  def peek: Option[A] =
    buff.headOption

  def add(a: A) = this.synchronized {
    buff = a :: buff
  }

  def get: Option[A] = this.synchronized {
    val v = buff.headOption
    if (buff.nonEmpty)
      buff = buff.tail
    v
  }

  def isEmpty: Boolean = buff.isEmpty

  def nonEmpty: Boolean = !isEmpty
}

object StreamBuffer {
  def empty[A]: StreamBuffer[A] = new StreamBuffer[A]
}
