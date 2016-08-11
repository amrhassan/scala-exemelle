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

  /** Takes n elements */
  def take(n: Int): StreamJob[Vector[Elem]] =
    if (n == 0)
      pure(Vector.empty)
    else for {
      elem ← next
      subsequent ← take(n-1)
    } yield elem.toVector ++ subsequent

  /** Drops n elements */
  def drop(n: Int): StreamJob[Unit] =
    if (n == 0)
      pure(())
    else
      next >> drop(n-1)

  /** Drops all elements until one satisfies predicate */
  def dropUntil(p: Elem ⇒ Boolean): StreamJob[Unit] =
    dropWhile(e ⇒ !p(e))

  /** Takes all elements until the specified element is found (exclusive) */
  def takeUntil(p: Elem ⇒ Boolean): StreamJob[Vector[Elem]] =
    takeWhile(e ⇒ !p(e))

  /** Takes a sequence of elements making up a tag given matchers for the [[StartTag]]
    * and [[EndTag]].
    */
  def takeTag(start: StartTag ⇒ Boolean)(end: EndTag ⇒ Boolean): StreamJob[Option[Tag]] =
    for {
      _ ← dropUntil { case tag: StartTag if start(tag) ⇒ true; case _ ⇒ false }
      elements ← takeUntilWithState[Int] { case (tag: EndTag, s) if s == 1 && end(tag) ⇒ true; case _ ⇒ false } (0) {
        case (Some(tag: StartTag), s) ⇒ s + 1
        case (Some(tag: EndTag), s) ⇒ s - 1
        case (_, s) ⇒ s
      }
      last ← if (elements.nonEmpty) take(1) else pure(Vector.empty)
      allElements = elements ++ last
    } yield if (allElements.nonEmpty) Some(Tag(allElements)) else None

  def takeTagNamed(name: String): StreamJob[Option[Tag]] =
    takeTag(_.name == name)(_.name == name)

  /** Takes all the tags with the given name */
  def takeTagsNamed(name: String): StreamJob[Vector[Tag]] =
    takeTagNamed(name) >>= { tag ⇒
      if (tag.isEmpty)
        pure(Vector.empty)
      else
        takeTagsNamed(name) map (tag.toVector ++ _)
    }

  def run[A](parser: StreamParser)(job: StreamJob[A])(implicit ec: ExecutionContext): Future[StreamError Xor A] =
    job.foldMap[XorT[Future, StreamError, ?]](parser).value
}

