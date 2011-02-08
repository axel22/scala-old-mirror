/* NSC -- new Scala compiler
 * Copyright 2002-2011 LAMP/EPFL
 * @author Martin Odersky
 */

package scala.tools.nsc
package reporters

import scala.tools.nsc.util._

/**
 * This interface provides methods to issue information, warning and
 * error messages.
 */
abstract class Reporter {
  object severity extends Enumeration
  class Severity(val id: Int) extends severity.Value {
    var count: Int = 0
  }
  val INFO = new Severity(0)
  val WARNING = new Severity(1)
  val ERROR = new Severity(2)

  def reset() {
    INFO.count = 0
    ERROR.count   = 0
    WARNING.count = 0
    cancelled = false
  }

  var cancelled: Boolean = false
  def hasErrors: Boolean = ERROR.count > 0 || cancelled
  def hasWarnings: Boolean = WARNING.count > 0

  /** Flush all output */
  def flush() { }

  protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean): Unit

  private var source: SourceFile = _
  def setSource(source: SourceFile) { this.source = source }
  def getSource: SourceFile = source
  def withSource[A](src: SourceFile)(op: => A) = {
    val oldSource = source
    try {
      source = src
      op 
    } finally { 
      source = oldSource 
    }
  }
  
  /** Whether very long lines can be truncated.  This exists so important
   *  debugging information (like printing the classpath) is not rendered
   *  invisible due to the max message length.
   */
  private var _truncationOK: Boolean = true
  def truncationOK = _truncationOK
  def withoutTruncating[T](body: => T): T = {
    val saved = _truncationOK
    _truncationOK = false
    try body
    finally _truncationOK = saved
  }

  def    info(pos: Position, msg: String, force: Boolean) { info0(pos, msg,    INFO, force) }
  def warning(pos: Position, msg: String                ) { info0(pos, msg, WARNING, false) }
  def   error(pos: Position, msg: String                ) { info0(pos, msg,   ERROR, false) }

  def comment(pos: Position, msg: String) {}

  /** An error that could possibly be fixed if the unit were longer.
   *  This is used only when the interpreter tries
   *  to distinguish fatal errors from those that are due to
   *  needing more lines of input from the user.
   *
   * Should be re-factored into a subclass.
   */
  var incompleteInputError: (Position, String) => Unit = error
  var incompleteHandled: Boolean = false
  
  def withIncompleteHandler[T](handler: (Position, String) => Unit)(thunk: => T) = {
    val savedHandler = incompleteInputError
    val savedHandled = incompleteHandled
    try {
      incompleteInputError = handler
      incompleteHandled = true
      thunk
    } finally {
      incompleteInputError = savedHandler
      incompleteHandled = savedHandled
    }
  }
  
  // @M: moved here from ConsoleReporter and made public -- also useful in e.g. Typers
  /** Returns a string meaning "n elements".
   *
   *  @param n        ...
   *  @param elements ...
   *  @return         ...
   */
  def countElementsAsString(n: Int, elements: String): String =
    n match {
      case 0 => "no "    + elements + "s"
      case 1 => "one "   + elements
      case 2 => "two "   + elements + "s"
      case 3 => "three " + elements + "s"
      case 4 => "four "  + elements + "s"
      case _ => "" + n + " " + elements + "s"
    }
    
  /** Turns a count into a friendly English description if n<=4. 
   *
   *  @param n        ...
   *  @return         ...
   */
  def countAsString(n: Int): String =
    n match {
      case 0 => "none"
      case 1 => "one"
      case 2 => "two"
      case 3 => "three"
      case 4 => "four"
      case _ => "" + n 
    }    
}
