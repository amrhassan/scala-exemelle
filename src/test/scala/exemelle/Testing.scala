package exemelle

import java.io.InputStream
import scala.concurrent.Await
import cats.data.Xor
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Testing {

  def breakfastMenu: InputStream = getClass.getResourceAsStream("/breakfast_menu.xml")

  def createBreakfastMenuParser = UnsafeStreamParser(breakfastMenu)

  def testRun[A](parser: StreamParser, action: StreamAction[A]): StreamError Xor A =
    Await.result(StreamAction.run(parser)(action), 1 minute)
}
