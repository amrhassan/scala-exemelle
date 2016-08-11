package exemelle

import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Xor, XorT}
import cats.free.Free
import cats.implicits._


sealed trait StreamOp[A]
case object Peek extends StreamOp[Option[Elem]]
case object Next extends StreamOp[Option[Elem]]

/** XML stream jobs */
object StreamJob {

  def pure[A](a: A): StreamJob[A] =
    Free.pure(a)

  /** Retrieves the next element */
  def next: StreamJob[Option[Elem]] =
    Free.liftF(Next)

  /** Peeks into the next element */
  def peek: StreamJob[Option[Elem]] =
    Free.liftF(Peek)

  /** Take elements from stream while p is true */
  def takeWhile(p: Elem ⇒ Boolean): StreamJob[Vector[Elem]] =
    peek >>= { peeked ⇒
      if (peeked exists p)
        for {
          elem ← next
          subsequent ← takeWhile(p)
        } yield elem.toVector ++ subsequent
      else
        pure(Vector.empty)
    }

  /** Drops elements from stream while predicate is true */
  def dropWhile(p: Elem ⇒ Boolean): StreamJob[Unit] =
    peek >>= { peeked ⇒
      if (peeked exists p)
        next >> dropWhile(p)
      else
        pure(())
    }

  def take(n: Int): StreamJob[Vector[Elem]] =
    if (n == 0)
      pure(Vector.empty)
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

  def extractNamed(name: String): StreamJob[Tag] =
    aggregate(_.name == name)(_.name == name)

  /** Extracts all the tags with the given name */
  def extractAllNamed(name: String): StreamJob[Vector[Tag]] =
    extractNamed(name) >>= { tag ⇒
      if (tag.isEmpty)
        pure(Vector.empty)
      else
        extractAllNamed(name) map (tag +: _)
    }

  def run[A](interpreter: Interpreter)(job: StreamJob[A])(implicit ec: ExecutionContext): Future[StreamError Xor A] =
    job.foldMap[XorT[Future, StreamError, ?]](interpreter).value
}
