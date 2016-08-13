package exemelle

import scala.concurrent.{ExecutionContext, Future}
import cats.data.{Xor, XorT}
import cats.free.Free
import cats.implicits._

/** Primitive stream operations */
sealed trait StreamOp[A]
case object Peek extends StreamOp[Option[Elem]]
case object Next extends StreamOp[Option[Elem]]

/** XML stream combinators */
object StreamAction {

  def pure[A](a: A): StreamAction[A] =
    Free.pure(a)

  /** Retrieves the next element */
  def next: StreamAction[Option[Elem]] =
    Free.liftF(Next)

  /** Peeks into the next element */
  def peek: StreamAction[Option[Elem]] =
    Free.liftF(Peek)

  /** Take elements from stream while p is true */
  def takeWhile(p: Elem ⇒ Boolean): StreamAction[Vector[Elem]] =
    takeWhileWithState[Boolean]((elem, s) ⇒ p(elem))(true)((elem, s) ⇒ true) // Boolean is a dummy type

  /** A fancier version of [[takeWhile]] that maintains a state between successive calls of takeWhile given
    * the initial state and the state transition based on the current state and current element.
    */
  def takeWhileWithState[S](p: (Elem, S) ⇒ Boolean)(s: S)(stateTransition: (Option[Elem], S) ⇒ S): StreamAction[Vector[Elem]] =
    peek >>= { peeked ⇒
      if (peeked exists (e ⇒ p(e, s)))
        for {
          elem ← next
          subsequent ← takeWhileWithState(p)(stateTransition(elem, s))(stateTransition)
        } yield elem.toVector ++ subsequent
      else
        pure(Vector.empty)
    }

  def takeUntilWithState[S](p: (Elem, S) ⇒ Boolean)(s: S)(stateTransition: (Option[Elem], S) ⇒ S): StreamAction[Vector[Elem]] =
    takeWhileWithState[S]((elem, s) ⇒ !p(elem, s))(s)(stateTransition)

  /** Drops elements from stream while predicate is true */
  def dropWhile(p: Elem ⇒ Boolean): StreamAction[Unit] =
    peek >>= { peeked ⇒
      if (peeked exists p)
        next >> dropWhile(p)
      else
        pure(())
    }

  /** Takes n elements */
  def take(n: Int): StreamAction[Vector[Elem]] =
    if (n == 0)
      pure(Vector.empty)
    else for {
      elem ← next
      subsequent ← take(n-1)
    } yield elem.toVector ++ subsequent

  /** Drops n elements */
  def drop(n: Int): StreamAction[Unit] =
    if (n == 0)
      pure(())
    else
      next >> drop(n-1)

  /** Drops all elements until one satisfies predicate */
  def dropUntil(p: Elem ⇒ Boolean): StreamAction[Unit] =
    dropWhile(e ⇒ !p(e))

  /** Takes all elements until the specified element is found (exclusive) */
  def takeUntil(p: Elem ⇒ Boolean): StreamAction[Vector[Elem]] =
    takeWhile(e ⇒ !p(e))

  /** Advances the element stream until it finds an element satisfying the [[StartTag]] predicate
    * then accumulates all the elements in memory until it finds a suitable closing tag satisfying
    * the [[EndTag]] predicate and returns those elements as a [[Tag]]
    *
    * Beware, this can blow up the memory consumption. Only use if you're sure the matched elements
    * can fit in application memory.
    */
  def findTag(start: StartTag ⇒ Boolean)(end: EndTag ⇒ Boolean): StreamAction[Option[Tag]] =
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

  /**
    * Advances the element stream until it finds a [[StartTag]] with the specified name and accumulates in memory
    * all the elements constituting the tag until a suitable matching [[EndTag]] is encountered
    *
    * Beware, this can blow up the memory consumption. Only use if you're sure all the encountered elements can
    * fit in application memory.
    */
  def findTagNamed(name: String): StreamAction[Option[Tag]] =
    findTag(_.name == name)(_.name == name)

  /** Like [[findTagNamed]] but keeps going until the end of the stream is encountered
    *
    * Beware, this can blow up the memory consumption. Only use if you're sure all the encountered elements can
    * fit in application memory.
    */
  def findAllTagsNamed(name: String): StreamAction[Vector[Tag]] =
    findTagNamed(name) >>= { tag ⇒
      if (tag.isEmpty)
        pure(Vector.empty)
      else
        findAllTagsNamed(name) map (tag.toVector ++ _)
    }

  def run[A](parser: StreamParser)(action: StreamAction[A])(implicit ec: ExecutionContext): Future[StreamError Xor A] =
    action.foldMap[XorT[Future, StreamError, ?]](parser).value
}

