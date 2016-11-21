import cats.data.EitherT
import cats.free.Free
import cats.~>

import scala.concurrent.Future

package object exemelle {
  type StreamAction[A] = Free[StreamOp, A]
  type StreamParser = StreamOp ~> EitherT[Future, StreamError, ?]
}
