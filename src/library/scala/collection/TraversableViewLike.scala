/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */



package scala.collection

import generic._
import mutable.{Builder, ArrayBuffer}
import TraversableView.NoBuilder
import annotation.migration


/** A template trait for non-strict views of traversable collections.
 *  $traversableviewinfo
 *
 *  Implementation note: Methods such as `map` or `flatMap` on this view will not invoke the implicitly passed
 *  `Builder` factory, but will return a new view directly, to preserve by-name behavior.
 *  The new view is then cast to the factory's result type. This means that every `CanBuildFrom`
 *  that takes a `View` as its `From` type parameter must yield the same view (or a generic
 *  superclass of it) as its result parameter. If that assumption is broken, cast errors might result.
 *
 * @define viewinfo
 *  A view is a lazy version of some collection. Collection transformers such as 
 *  `map` or `filter` or `++` do not traverse any elements when applied on a view.
 *  Instead they create a new view which simply records that fact that the operation
 *  needs to be applied. The collection elements are accessed, and the view operations are applied,
 *  when a non-view result is needed, or when the `force` method is called on a view.
 * @define traversableviewinfo
 *  $viewinfo
 *
 *  All views for traversable collections are defined by creating a new `foreach` method.

 *  @author Martin Odersky
 *  @version 2.8
 *  @since   2.8
 *  @tparam A    the element type of the view
 *  @tparam Coll the type of the underlying collection containing the elements.
 *  @tparam This the type of the view itself
 */
trait TraversableViewLike[+A, 
                          +Coll, 
                          +This <: TraversableView[A, Coll] with TraversableViewLike[A, Coll, This]]
  extends Traversable[A] with TraversableLike[A, This] { 
self =>

  override protected[this] def newBuilder: Builder[A, This] =    
    throw new UnsupportedOperationException(this+".newBuilder")

  protected def underlying: Coll

  def force[B >: A, That](implicit bf: CanBuildFrom[Coll, B, That]) = {
    val b = bf(underlying)
    b ++= this
    b.result()
  }

  /** The implementation base trait of this view.
   *  This trait and all its subtraits has to be re-implemented for each
   *  ViewLike class. 
   */
  trait Transformed[+B] extends TraversableView[B, Coll] {
    lazy val underlying = self.underlying
    override def toString = stringPrefix+"(...)"
  }

  /** A fall back which forces everything into a vector and then applies an operation
   *  on it. Used for those operations which do not naturally lend themselves to a view
   */
  trait Forced[B] extends Transformed[B] {
    protected[this] def forced: Seq[B]
    private[this] lazy val forcedCache = forced
    override def foreach[U](f: B => U) = forcedCache.foreach(f)
    override def stringPrefix = self.stringPrefix+"C"
  }

  /** pre: from >= 0  
   */
  trait Sliced extends Transformed[A] {
    protected[this] val from: Int
    protected[this] val until: Int
    override def foreach[U](f: A => U) {
      var index = 0
      for (x <- self) {
        if (from <= index) {
          if (until <= index) return
          f(x)
        }
        index += 1
      }
    }
    override def stringPrefix = self.stringPrefix+"S"
    override def slice(from1: Int, until1: Int): This =
      newSliced(from1 max 0, until1 max 0).asInstanceOf[This]
  }

  trait Mapped[B] extends Transformed[B] {
    protected[this] val mapping: A => B
    override def foreach[U](f: B => U) {
      for (x <- self)
        f(mapping(x))
    }
    override def stringPrefix = self.stringPrefix+"M"
  }

  trait FlatMapped[B] extends Transformed[B] {
    protected[this] val mapping: A => TraversableOnce[B]
    override def foreach[U](f: B => U) {
      for (x <- self)
        for (y <- mapping(x))
          f(y)
    }
    override def stringPrefix = self.stringPrefix+"N"
  }

  trait Appended[B >: A] extends Transformed[B] {
    protected[this] val rest: Traversable[B]
    override def foreach[U](f: B => U) {
      for (x <- self) f(x)
      for (x <- rest) f(x)
    }
    override def stringPrefix = self.stringPrefix+"A"
  }    

  trait Filtered extends Transformed[A] {
    protected[this] val pred: A => Boolean 
    override def foreach[U](f: A => U) {
      for (x <- self)
        if (pred(x)) f(x)
    }
    override def stringPrefix = self.stringPrefix+"F"
  }

  trait TakenWhile extends Transformed[A] {
    protected[this] val pred: A => Boolean 
    override def foreach[U](f: A => U) {
      for (x <- self) {
        if (!pred(x)) return
        f(x)
      }
    }
    override def stringPrefix = self.stringPrefix+"T"
  }

  trait DroppedWhile extends Transformed[A] {
    protected[this] val pred: A => Boolean 
    override def foreach[U](f: A => U) {
      var go = false
      for (x <- self) {
        if (!go && !pred(x)) go = true
        if (go) f(x)
      }
    }
    override def stringPrefix = self.stringPrefix+"D"
  }

  /** Boilerplate method, to override in each subclass
   *  This method could be eliminated if Scala had virtual classes
   */
  protected def newForced[B](xs: => Seq[B]): Transformed[B] = new Forced[B] { val forced = xs }
  protected def newAppended[B >: A](that: Traversable[B]): Transformed[B] = new Appended[B] { val rest = that }
  protected def newMapped[B](f: A => B): Transformed[B] = new Mapped[B] { val mapping = f }
  protected def newFlatMapped[B](f: A => TraversableOnce[B]): Transformed[B] = new FlatMapped[B] { val mapping = f }
  protected def newFiltered(p: A => Boolean): Transformed[A] = new Filtered { val pred = p }
  protected def newSliced(_from: Int, _until: Int): Transformed[A] = new Sliced { val from = _from; val until = _until }
  protected def newDroppedWhile(p: A => Boolean): Transformed[A] = new DroppedWhile { val pred = p }
  protected def newTakenWhile(p: A => Boolean): Transformed[A] = new TakenWhile { val pred = p }
  
  override def ++[B >: A, That](xs: TraversableOnce[B])(implicit bf: CanBuildFrom[This, B, That]): That = {
    newAppended(xs.toTraversable).asInstanceOf[That]
// was:    if (bf.isInstanceOf[ByPassCanBuildFrom]) newAppended(that).asInstanceOf[That]
//         else super.++[B, That](that)(bf) 
  }

  override def map[B, That](f: A => B)(implicit bf: CanBuildFrom[This, B, That]): That = {
    newMapped(f).asInstanceOf[That]
//    val b = bf(repr)
//          if (b.isInstanceOf[NoBuilder[_]]) newMapped(f).asInstanceOf[That]
//    else super.map[B, That](f)(bf) 
  }

  override def collect[B, That](pf: PartialFunction[A, B])(implicit bf: CanBuildFrom[This, B, That]): That =
    filter(pf.isDefinedAt).map(pf)(bf)

  override def flatMap[B, That](f: A => TraversableOnce[B])(implicit bf: CanBuildFrom[This, B, That]): That = {
    newFlatMapped(f).asInstanceOf[That]
// was:    val b = bf(repr)
//     if (b.isInstanceOf[NoBuilder[_]]) newFlatMapped(f).asInstanceOf[That]
//    else super.flatMap[B, That](f)(bf)
  }

  protected[this] def thisSeq: Seq[A] = {
    val buf = new ArrayBuffer[A]
    self foreach (buf +=)
    buf.result
  }
  
  // Have to overload all three to work around #4299.  The overload
  // is because mkString should force a view but toString should not.
  override def mkString: String = mkString("")
  override def mkString(sep: String): String = mkString("", sep, "")
  override def mkString(start: String, sep: String, end: String): String = {
    thisSeq.addString(new StringBuilder(), start, sep, end).toString
  }
  
  override def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder =
    b append start append "..." append end

  override def toString = stringPrefix+"(...)"

  override def filter(p: A => Boolean): This = newFiltered(p).asInstanceOf[This]
  override def withFilter(p: A => Boolean): This = newFiltered(p).asInstanceOf[This]
  override def partition(p: A => Boolean): (This, This) = (filter(p), filter(!p(_)))
  override def init: This = newSliced(0, size - 1).asInstanceOf[This]
  override def drop(n: Int): This = newSliced(n max 0, Int.MaxValue).asInstanceOf[This]
  override def take(n: Int): This = newSliced(0, n).asInstanceOf[This]
  override def slice(from: Int, until: Int): This = newSliced(from max 0, until).asInstanceOf[This]
  override def dropWhile(p: A => Boolean): This = newDroppedWhile(p).asInstanceOf[This]
  override def takeWhile(p: A => Boolean): This = newTakenWhile(p).asInstanceOf[This]
  override def span(p: A => Boolean): (This, This) = (takeWhile(p), dropWhile(p))
  override def splitAt(n: Int): (This, This) = (take(n), drop(n))

  override def scanLeft[B, That](z: B)(op: (B, A) => B)(implicit bf: CanBuildFrom[This, B, That]): That =
    newForced(thisSeq.scanLeft(z)(op)).asInstanceOf[That]
  
  @migration(2, 9,
    "This scanRight definition has changed in 2.9.\n" +
    "The previous behavior can be reproduced with scanRight.reverse."
  )
  override def scanRight[B, That](z: B)(op: (A, B) => B)(implicit bf: CanBuildFrom[This, B, That]): That =
    newForced(thisSeq.scanRight(z)(op)).asInstanceOf[That]

  override def groupBy[K](f: A => K): immutable.Map[K, This] =
    thisSeq.groupBy(f).mapValues(xs => newForced(xs).asInstanceOf[This])
  
  override def stringPrefix = "TraversableView"
}


