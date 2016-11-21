package exemelle

import org.specs2._
import org.specs2.matcher.EitherMatchers

class UnsafeStreamParserSpec extends Specification with EitherMatchers { def is = s2"""
  First element is a StartDocument    $firstElement
  Next-ing order is correct           $nexting
  Peek-ing is correct                 $peeking
  """

  def firstElement =
    Testing.createBreakfastMenuParser.getNext() must beLike { case Right(Some(elem: StartDocument)) ⇒ ok }

  def nexting = {
    val parser = Testing.createBreakfastMenuParser
    val _ = parser.getNext()

    val first =
      parser.getNext() must beLike { case Right(Some(elem: StartTag)) ⇒ elem.text must beEqualTo("<breakfast_menu>") }

    val second =
      parser.getNext() must beLike { case Right(Some(elem: Text)) ⇒ ok }

    val third =
      parser.getNext() must beLike { case Right(Some(elem: StartTag)) ⇒ elem.text must beEqualTo("<food>") }

    first and second and third
  }

  def peeking = {
    val parser = Testing.createBreakfastMenuParser

    val peekFirst =
      parser.peek() must beLike { case Right(Some(elem: StartDocument)) ⇒ ok }

    val peekSecond =
      parser.peek() must beLike { case Right(Some(elem: StartDocument)) ⇒ ok }

    val first =
      parser.getNext() must beLike { case Right(Some(elem: StartDocument)) ⇒ ok }

    val second =
      parser.getNext() must beLike { case Right(Some(elem: StartTag)) ⇒ elem.text must beEqualTo("<breakfast_menu>") }

    val peekThird =
      parser.peek() must beLike { case Right(Some(elem: Text)) ⇒ ok }

    val third =
      parser.peek() must beLike { case Right(Some(elem: Text)) ⇒ ok }

    peekFirst and peekSecond and first and second and peekThird and third
  }
}
