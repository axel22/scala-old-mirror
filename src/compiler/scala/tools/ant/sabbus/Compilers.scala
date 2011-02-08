/*                     __                                               *\
**     ________ ___   / /  ___     Scala Ant Tasks                      **
**    / __/ __// _ | / /  / _ |    (c) 2005-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */


package scala.tools.ant.sabbus

import java.net.URL

object Compilers extends collection.DefaultMap[String, Compiler] {
  
  val debug = false
  
  private val container = new collection.mutable.HashMap[String, Compiler]
  
  def iterator = container.iterator	
  
  def get(id: String) = container.get(id)
  
  override def size = container.size
  
  def make(id: String, classpath: Array[URL], settings: Settings): Compiler = {
    val runtime = Runtime.getRuntime
    if (debug) println("Making compiler " + id)
    if (debug) println("  memory before: " + (runtime.freeMemory/1048576.).formatted("%10.2f") + " MB")
    val comp = new Compiler(classpath, settings)
    container += Pair(id, comp)
    if (debug) println("  memory after: " + (runtime.freeMemory/1048576.).formatted("%10.2f") + " MB")
    comp
  }
  
  def break(id: String): Null = {
    val runtime = Runtime.getRuntime
    if (debug) println("Breaking compiler " + id)
    if (debug) println("  memory before: " + (runtime.freeMemory/1048576.).formatted("%10.2f") + " MB")
    container -= id
    System.gc
    if (debug) println("  memory after: " + (runtime.freeMemory/1048576.).formatted("%10.2f") + " MB")
    null
  }
}
