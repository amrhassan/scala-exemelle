import cats.free.Free
import cats.~>


package object exemelle {
  type StreamAction[A] = Free[StreamOp, A]
  type StreamParser = StreamOp ~> Either[StreamError, ?]
}
