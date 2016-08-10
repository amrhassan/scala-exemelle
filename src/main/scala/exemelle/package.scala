import cats.free.Free

package object exemelle {
  type StreamJob[A] = Free[StreamOp, A]
}
