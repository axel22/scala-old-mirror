package scala.collection.parallel


import scala.collection.Parallel
import scala.collection.mutable.Builder
import scala.collection.generic.Sizing



/** The base trait for all combiners.
 *  A combiner lets one construct collections incrementally just like
 *  a regular builder, but also implements an efficient merge operation of two builders
 *  via `combine` method. Once the collection is constructed, it may be obtained by invoking
 *  the `result` method.
 *  
 *  @tparam Elem   the type of the elements added to the builder
 *  @tparam To     the type of the collection the builder produces
 *  
 *  @author prokopec
 */
trait Combiner[-Elem, +To] extends Builder[Elem, To] with Sizing with Parallel {
self: EnvironmentPassingCombiner[Elem, To] =>
  private[collection] final val tasksupport = getTaskSupport
  
  // type EPC = EnvironmentPassingCombiner[Elem, To]
  //
  // [scalacfork] /scratch/trunk2/src/library/scala/collection/parallel/Combiner.scala:25: error: contravariant type Elem occurs in invariant position in type scala.collection.parallel.EnvironmentPassingCombiner[Elem,To] of type EPC
  // [scalacfork]   type EPC = EnvironmentPassingCombiner[Elem, To]
  // [scalacfork]        ^
  // [scalacfork] one error found  
  
  /** Combines the contents of the receiver builder and the `other` builder,
   *  producing a new builder containing both their elements.
   *  
   *  This method may combine the two builders by copying them into a larger collection,
   *  by producing a lazy view that gets evaluated once `result` is invoked, or use
   *  a merge operation specific to the data structure in question.
   *  
   *  Note that both the receiver builder and `other` builder become invalidated
   *  after the invocation of this method, and should be cleared (see `clear`)
   *  if they are to be used again.
   *
   *  Also, combining two combiners `c1` and `c2` for which `c1 eq c2` is `true`, that is,
   *  they are the same objects in memory:
   *
   *  {{{
   *  c1.combine(c2)
   *  }}}
   *  
   *  always does nothing and returns `c1`.
   *  
   *  @tparam N      the type of elements contained by the `other` builder
   *  @tparam NewTo  the type of collection produced by the `other` builder
   *  @param other   the other builder
   *  @return        the parallel builder containing both the elements of this and the `other` builder
   */
  def combine[N <: Elem, NewTo >: To](other: Combiner[N, NewTo]): Combiner[N, NewTo]
  
}


trait EnvironmentPassingCombiner[-Elem, +To] extends Combiner[Elem, To] {
  abstract override def result = {
    val res = super.result
    // 
    res
  }
}










