package exemelle

import cats.data.Xor
import org.specs2._
import org.specs2.matcher.XorMatchers

class UnsafeStreamParserSpec extends Specification with XorMatchers { def is = s2"""
  First element is a StartDocument    $firstElement
  Next-ing order is correct           $nexting
  Peek-ing is correct                 $peeking
  """

  def firstElement =
    newParser.getNext() must beLike { case Xor.Right(Some(elem: StartDocument)) ⇒ ok }

  def nexting = {
    val parser = newParser
    val _ = parser.getNext()

    val first =
      parser.getNext() must beLike { case Xor.Right(Some(elem: StartTag)) ⇒ elem.text must beEqualTo("<breakfast_menu>") }

    val second =
      parser.getNext() must beLike { case Xor.Right(Some(elem: Text)) ⇒ ok }

    val third =
      parser.getNext() must beLike { case Xor.Right(Some(elem: StartTag)) ⇒ elem.text must beEqualTo("<food>") }

    first and second and third
  }

  def peeking = {
    val parser = newParser

    val peekFirst =
      parser.peek() must beLike { case Xor.Right(Some(elem: StartDocument)) ⇒ ok }

    val peekSecond =
      parser.peek() must beLike { case Xor.Right(Some(elem: StartDocument)) ⇒ ok }

    val first =
      parser.getNext() must beLike { case Xor.Right(Some(elem: StartDocument)) ⇒ ok }

    val second =
      parser.getNext() must beLike { case Xor.Right(Some(elem: StartTag)) ⇒ elem.text must beEqualTo("<breakfast_menu>") }

    val peekThird =
      parser.peek() must beLike { case Xor.Right(Some(elem: Text)) ⇒ ok }

    val third =
      parser.peek() must beLike { case Xor.Right(Some(elem: Text)) ⇒ ok }

    peekFirst and peekSecond and first and second and peekThird and third
  }

  def newParser = UnsafeStreamParser(inputStream)
  def inputStream = ClassLoader.getSystemResourceAsStream("breakfast_menu.xml")
}
