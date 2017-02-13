package exemelle

import java.io.InputStream

object Testing {

  def breakfastMenu: InputStream = getClass.getResourceAsStream("/breakfast_menu.xml")

  def createBreakfastMenuParser = UnsafeStreamParser(breakfastMenu)

  def testRun[A](parser: StreamParser, action: StreamAction[A]): Either[StreamError, A] =
    StreamAction.run(parser)(action)
}
