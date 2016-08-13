import scala.concurrent.Future
import cats.data.XorT
import cats.free.Free
import cats.~>

package object exemelle {
  type StreamAction[A] = Free[StreamOp, A]
  type StreamParser = StreamOp ~> XorT[Future, StreamError, ?]
}
