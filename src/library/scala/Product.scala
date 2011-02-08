/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala

/** Base trait for all products.  See [[scala.Product2]].
 *
 *  @author  Burak Emir
 *  @version 1.0
 *  @since   2.3
 */
trait Product extends Equals {

  /** Returns the nth element of this product, 0-based.  In other words, for a 
   * product <code>A(x_1,...,x_k)</code>, returns <code>x_(n+1)</code>
   * where <code>0 &lt;= n &lt; k</code>
   *
   *  @param  n the index of the element to return
   *  @throws IndexOutOfBoundsException
   *  @return  The element <code>n</code> elements after the first element
   */
  def productElement(n: Int): Any

  /** Returns the size of this product.
   * @return For a product <code>A(x_1,...,x_k)</code>, returns `k`
   */
  def productArity: Int

  /** An iterator that returns all fields of this product */
  def productIterator: Iterator[Any] = new Iterator[Any] {
    private var c: Int = 0
    private val cmax = productArity
    def hasNext = c < cmax
    def next() = { val result = productElement(c); c += 1; result }
  }
  
  @deprecated("use productIterator instead")
  def productElements: Iterator[Any] = productIterator

  /** 
   * Returns a string that is used in the `toString` method of subtraits/classes.
   *  Implementations may override this
   *  method in order to prepend a string prefix to the result of the 
   *  toString methods. 
   *  @return the empty string
   */
  def productPrefix = ""
}
