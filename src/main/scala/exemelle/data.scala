package exemelle

sealed trait Elem {
  def text: String
}

case class StartTag(name: String, attributes: List[Attribute], text: String) extends Elem
case class EndTag(name: String, text: String) extends Elem
case class StartDocument(text: String) extends Elem
case class Text(text: String) extends Elem
case class Other(eventType: Int, text: String) extends Elem

case class Attribute(name: String, value: String)

case class Tag(elems: Vector[Elem]) {
  def originalText: String = elems.map(_.text).mkString
}

/** An XML error */
case class StreamError(message: String)
