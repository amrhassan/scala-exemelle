package exemelle

import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Xor, XorT}
import cats.free.Free
import cats.~>
import cats.implicits._

sealed trait StreamOp[A]

case class DropWhile(p: Elem ⇒ Boolean) extends StreamOp[Unit]
case class TakeWhile(p: Elem ⇒ Boolean) extends StreamOp[Vector[Elem]]

case object Peek extends StreamOp[Option[Elem]]
case object Next extends StreamOp[Option[Elem]]

/** XML stream jobs */
object StreamJob {

  def pure[A](a: A): StreamJob[A] =
    Free.pure(a)

  def next: StreamJob[Option[Elem]] =
    Free.liftF(Next)

  /** Take elements from stream while p is true */
  def takeWhile(p: Elem ⇒ Boolean): StreamJob[Vector[Elem]] =
    Free.liftF(TakeWhile(p))

  /** Drops elements from stream while predicate is true */
  def dropWhile(p: Elem ⇒ Boolean): StreamJob[Unit] =
    Free.liftF(DropWhile(p))

  def take(n: Int): StreamJob[Vector[Elem]] =
    if (n == 0)
      Free.pure(Vector.empty)
    else for {
      elem ← next
      subsequent ← take(n-1)
    } yield elem.toVector ++ subsequent

  /** Drops all elements until one satisfies predicate */
  def dropUntil(p: Elem ⇒ Boolean): StreamJob[Unit] =
    dropWhile(e ⇒ !p(e))

  /** Takes all elements until the specified element is found (exclusive) */
  def takeUntil(p: Elem ⇒ Boolean): StreamJob[Vector[Elem]] =
    takeWhile(e ⇒ !p(e))

  /** Aggregates a sequence of elements defined by a starting tag and a subsequent ending tag */
  def aggregate(start: StartTag ⇒ Boolean)(end: EndTag ⇒ Boolean): StreamJob[Vector[Elem]] =
    for {
      _ ← dropUntil { case tag: StartTag if start(tag) ⇒ true; case _ ⇒ false }
      elements ← takeUntil { case tag: EndTag if end(tag) ⇒ true; case _ ⇒ false }
      last ← take(1)
    } yield elements ++ last

  def findTagNamed(name: String): StreamJob[Vector[Elem]] =
    aggregate(_.name == name)(_.name == name)

  def run[A](interpreter: Interpreter)(job: StreamJob[A])(implicit ec: ExecutionContext): Future[StreamError Xor A] =
    job.foldMap[XorT[Future, StreamError, ?]](interpreter).value
}
