/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */



package scala.collection
package immutable

import generic._
import mutable.Builder

/** A subtrait of `collection.Seq` which represents sequences
 *  that are guaranteed immutable.
 *
 *  $seqInfo
 *  @define Coll immutable.Seq
 *  @define coll immutable sequence
 */
trait Seq[+A] extends Iterable[A] 
                      with scala.collection.Seq[A] 
                      with GenericTraversableTemplate[A, Seq]
                      with SeqLike[A, Seq[A]] { 
  override def companion: GenericCompanion[Seq] = Seq
  override def toSeq: Seq[A] = this
}

/** $factoryInfo
 *  @define Coll immutable.Seq
 *  @define coll immutable sequence
 */
object Seq extends SeqFactory[Seq] {
  /** genericCanBuildFromInfo */
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Seq[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, Seq[A]] = new mutable.ListBuffer
}
