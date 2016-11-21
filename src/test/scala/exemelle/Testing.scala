package exemelle

import java.io.InputStream
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Testing {

  def breakfastMenu: InputStream = getClass.getResourceAsStream("/breakfast_menu.xml")

  def createBreakfastMenuParser = UnsafeStreamParser(breakfastMenu)

  def testRun[A](parser: StreamParser, action: StreamAction[A]): Either[StreamError, A] =
    Await.result(StreamAction.run(parser)(action), 1 minute)
}
