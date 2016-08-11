import scala.concurrent.Future
import cats.data.XorT
import cats.free.Free
import cats.~>

package object exemelle {
  type StreamJob[A] = Free[StreamOp, A]
  type StreamParser = StreamOp ~> XorT[Future, StreamError, ?]
}
