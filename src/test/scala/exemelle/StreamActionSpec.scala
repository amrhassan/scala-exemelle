package exemelle

import org.specs2._
import Testing._
import StreamAction._


class StreamActionSpec extends Specification { def is = s2"""
  Finds named full tags correctly     $findingNamedTag
  """

  def findingNamedTag = {
    val action = findTagNamed("food")

    val parser = StreamParser.fromInputStream(breakfastMenu)

    val first =
      testRun(parser, action) must beLike {
        case Right(Some(tag)) ⇒ tag.originalText must be equalTo
          """<food>
            |        <food><name>This is fake</name></food>
            |        <name>Belgian Waffles</name>
            |        <price>$5.95</price>
            |        <description>
            |            Two of our famous Belgian Waffles with plenty of real maple syrup
            |        </description>
            |        <calories>650</calories>
            |    </food>""".stripMargin
      }

    val second =
      testRun(parser, action) must beLike {
        case Right(Some(tag)) ⇒ tag.originalText must be equalTo
          """<food>
            |        <food><name>This is fake</name></food>
            |        <name>Strawberry Belgian Waffles</name>
            |        <price>$7.95</price>
            |        <description>
            |            Light Belgian waffles covered with strawberries and whipped cream
            |        </description>
            |        <calories>900</calories>
            |    </food>""".stripMargin
      }

    val third = testRun(parser, action) must beLike { case Right(None) ⇒ ok }

    first and second and third
  }
}
