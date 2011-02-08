/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */
package scala.collection
package mutable

import generic._

/** A base trait for iterable collections that can be mutated.
 *  $iterableInfo
 */
trait Iterable[A] extends Traversable[A] 
                     with scala.collection.Iterable[A] 
                     with GenericTraversableTemplate[A, Iterable]
                     with IterableLike[A, Iterable[A]] { 
  override def companion: GenericCompanion[Iterable] = Iterable
}	

/** $factoryInfo
 *  The current default implementation of a $Coll is an `ArrayBuffer`.
 *  @define coll mutable iterable collection
 *  @define Coll mutable.Iterable
 */
object Iterable extends TraversableFactory[Iterable] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Iterable[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, Iterable[A]] = new ArrayBuffer
}

