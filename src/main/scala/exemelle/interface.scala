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
    takeWhileWithState[Boolean]((elem, s) ⇒ p(elem))(true)((elem, s) ⇒ true) // Boolean is a dummy type

//  def takeWhile(p: Elem ⇒ Boolean): StreamJob[Vector[Elem]] =
//    peek >>= { peeked ⇒
//      if (peeked exists p)
//        for {
//          elem ← next
//          subsequent ← takeWhile(p)
//        } yield elem.toVector ++ subsequent
//      else
//        pure(Vector.empty)
//    }

  /** A fancier version of takeWhile that maintains a state between successive calls of takeWhile given
    * the initial state and the state transition based on the current state and current element.
    */
  def takeWhileWithState[S](p: (Elem, S) ⇒ Boolean)(s: S)(stateTransition: (Option[Elem], S) ⇒ S): StreamJob[Vector[Elem]] =
    peek >>= { peeked ⇒
      if (peeked exists (e ⇒ p(e, s)))
        for {
          elem ← next
          subsequent ← takeWhileWithState(p)(stateTransition(elem, s))(stateTransition)
        } yield elem.toVector ++ subsequent
      else
        pure(Vector.empty)
    }

  def takeUntilWithState[S](p: (Elem, S) ⇒ Boolean)(s: S)(stateTransition: (Option[Elem], S) ⇒ S): StreamJob[Vector[Elem]] =
    takeWhileWithState[S]((elem, s) ⇒ !p(elem, s))(s)(stateTransition)

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
      elements ← takeUntilWithState[Int] { case (tag: EndTag, s) if s == 1 && end(tag) ⇒ true; case _ ⇒ false } (0) {
        case (Some(tag: StartTag), s) ⇒ s + 1
        case (Some(tag: EndTag), s) ⇒ s - 1
        case (_, s) ⇒ s
      }
      last ← take(1)
    } yield elements ++ last

  def extractNamed(name: String): StreamJob[Option[Tag]] =
    aggregate(_.name == name)(_.name == name) map { elems ⇒
      if (elems.nonEmpty) Some(Tag(elems)) else None
    }

  /** Extracts all the tags with the given name */
  def extractAllNamed(name: String): StreamJob[Vector[Tag]] =
    extractNamed(name) >>= { tag ⇒
      if (tag.isEmpty)
        pure(Vector.empty)
      else
        extractAllNamed(name) map (tag.toVector ++ _)
    }

  def run[A](interpreter: Interpreter)(job: StreamJob[A])(implicit ec: ExecutionContext): Future[StreamError Xor A] =
    job.foldMap[XorT[Future, StreamError, ?]](interpreter).value
}
