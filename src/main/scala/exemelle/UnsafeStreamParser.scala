package exemelle

import java.io.{InputStream, StringWriter}
import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}
import javax.xml.stream.{XMLEventReader, XMLInputFactory, XMLStreamConstants, XMLStreamException}
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Xor, XorT}
import cats.implicits._


/** An XML stream parser that changes the state of its underlying InputStream */
case class UnsafeStreamParser private(private val reader: XMLEventReader) {

  /** Prepended elements to the stream */
  private var heads: List[Elem] = List.empty

  /** Possibly retrieves the next element in the XML stream and effectively advancing the underlying
    * InputStream
    */
  def getNext: StreamError Xor Option[Elem] = this.synchronized {
    if (heads.nonEmpty) {
      val r = heads.headOption.right
      heads = heads.tail
      r
    } else {
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
    for {
      next ← getNext
    } yield {
      next foreach (elem ⇒ prepend(elem))
      next
    }
  }

  private def prepend(elem: Elem): Unit =
    heads = elem :: heads

  private def unsafeNextElement: Elem = {
    val event = reader.nextEvent()
    event.getEventType match {
      case XMLStreamConstants.START_ELEMENT ⇒ startTag(event.asStartElement())
      case XMLStreamConstants.END_ELEMENT ⇒ endTag(event.asEndElement())
      case XMLStreamConstants.START_DOCUMENT ⇒ startDocument(event)
      case XMLStreamConstants.CHARACTERS ⇒ text(event)
      case _ ⇒ Other(event.getEventType, event.toString)
    }
  }

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

  def apply(in: InputStream): UnsafeStreamParser =
    UnsafeStreamParser(XMLInputFactory.newInstance().createXMLEventReader(in))

  def interpreter(parser: UnsafeStreamParser)(implicit ec: ExecutionContext): Interpreter = new Interpreter {

    def apply[A](op: StreamOp[A]): XorT[Future, StreamError, A] = op match {
      case Take(n) ⇒ XorT(Future(take(n)))
      case DropWhile(p) ⇒ XorT(Future(dropWhile(p)))
      case TakeWhile(p) ⇒ XorT(Future(takeWhile(p)))
    }

    def take(n: Int): StreamError Xor Vector[Elem] =
      if (n == 0)
        Vector.empty.right
      else for {
        next ← parser.getNext
        subsequent ← take(n-1)
      } yield next.toVector ++ subsequent

    def dropWhile(p: Elem ⇒ Boolean): StreamError Xor Unit = {
      parser.peek >>= { peeked ⇒
        if (peeked exists p)
          { parser.getNext; dropWhile(p) }
        else
          Xor.right(())
      }
    }

    def takeWhile(p: Elem ⇒ Boolean): StreamError Xor Vector[Elem] = {
      parser.peek >>= { peeked ⇒
        if (peeked exists p)
          for {
            next ← parser.getNext
            subsequent ← takeWhile(p)
          } yield next.toVector ++ subsequent
        else
          Xor.right(Vector.empty)
      }
    }
  }
}
