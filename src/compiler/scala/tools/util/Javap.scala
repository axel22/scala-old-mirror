/* NSC -- new Scala compiler
 * Copyright 2005-2011 LAMP/EPFL
 * @author Paul Phillips
 */
 
package scala.tools
package util

import java.lang.reflect.{ GenericSignatureFormatError, Method, Constructor }
import java.lang.{ ClassLoader => JavaClassLoader }
import scala.tools.nsc.util.ScalaClassLoader
import java.io.{ InputStream, PrintWriter, ByteArrayInputStream, FileNotFoundException }
import scala.tools.nsc.io.{ File, NullPrintStream }
import Javap._

class Javap(
  val loader: ScalaClassLoader = ScalaClassLoader.getSystemLoader(),
  val printWriter: PrintWriter = new PrintWriter(System.out, true)
) {
  
  lazy val parser = new JpOptions
  
  val EnvClass = loader.tryToInitializeClass[FakeEnvironment](Env).orNull
  val EnvCtr   = EnvClass.getConstructor(List[Class[_]](): _*)

  val PrinterClass = loader.tryToInitializeClass[FakePrinter](Printer).orNull
  val PrinterCtr   = PrinterClass.getConstructor(classOf[InputStream], classOf[PrintWriter], EnvClass)

  def findBytes(path: String): Array[Byte] =
    tryFile(path) getOrElse tryClass(path)
  
  def apply(args: Seq[String]): List[JpResult] = {    
    args.toList filterNot (_ startsWith "-") map { path =>
      val bytes = findBytes(path)
      if (bytes.isEmpty) new JpError("Could not find class bytes for '%s'".format(path))
      else new JpSuccess(newPrinter(new ByteArrayInputStream(bytes), newEnv(args)))
    }
  }
  
  def newPrinter(in: InputStream, env: FakeEnvironment): FakePrinter =
    PrinterCtr.newInstance(in, printWriter, env)

  def newEnv(opts: Seq[String]): FakeEnvironment = {
    val env: FakeEnvironment = EnvClass.newInstance()

    parser(opts) foreach { case (name, value) =>
      val field = EnvClass getDeclaredField name
      field setAccessible true
      field.set(env, value.asInstanceOf[AnyRef])
    }

    env
  }
  
  /** Assume the string is a path and try to find the classfile
   *  it represents.
   */
  def tryFile(path: String): Option[Array[Byte]] = {
    val file = File(
      if (path.endsWith(".class")) path
      else path.replace('.', '/') + ".class"
    )    
    if (!file.exists) None
    else try Some(file.toByteArray) catch { case x: Exception => None }
  }
  /** Assume the string is a fully qualified class name and try to
   *  find the class object it represents.
   */
  def tryClass(path: String): Array[Byte] = {
    val extName = (
      if (path endsWith ".class") (path dropRight 6).replace('/', '.')
      else path
    )
    loader.classBytes(extName)
  }
}

object Javap {
  val Env     = "sun.tools.javap.JavapEnvironment"
  val Printer = "sun.tools.javap.JavapPrinter"

  def isAvailable(cl: ScalaClassLoader = ScalaClassLoader.getSystemLoader()) =
    cl.tryToInitializeClass[AnyRef](Env).isDefined

  // "documentation"
  type FakeEnvironment = AnyRef
  type FakePrinter = AnyRef

  def apply(path: String): Unit      = apply(Seq(path))
  def apply(args: Seq[String]): Unit = new Javap() apply args foreach (_.show())
  
  sealed trait JpResult {
    type ResultType
    def isError: Boolean
    def value: ResultType
    def show(): Unit
    // todo
    // def header(): String
    // def fields(): List[String]
    // def methods(): List[String]
    // def signatures(): List[String]
  }
  class JpError(msg: String) extends JpResult {
    type ResultType = String
    def isError = true
    def value = msg
    def show() = println(msg)
  }
  class JpSuccess(val value: AnyRef) extends JpResult {
    type ResultType = AnyRef
    def isError = false
    def show() = value.asInstanceOf[{ def print(): Unit }].print()
  }
  
  class JpOptions {
    private object Access {
      final val PRIVATE = 0
      final val PROTECTED = 1
      final val PACKAGE = 2
      final val PUBLIC = 3
    }
    private val envActionMap: Map[String, (String, Any)] = {
      val map = Map(
        "-l"         -> (("showLineAndLocal", true)),
        "-c"         -> (("showDisassembled", true)),
        "-s"         -> (("showInternalSigs", true)),
        "-verbose"   -> (("showVerbose", true)),
        "-private"   -> (("showAccess", Access.PRIVATE)),
        "-package"   -> (("showAccess", Access.PACKAGE)),
        "-protected" -> (("showAccess", Access.PROTECTED)),
        "-public"    -> (("showAccess", Access.PUBLIC)),
        "-all"       -> (("showallAttr", true))
      )
      map ++ List(
        "-v" -> map("-verbose"),
        "-p" -> map("-private")
      )
    }
    def apply(opts: Seq[String]): Seq[(String, Any)] = {
      opts flatMap { opt =>
        envActionMap get opt match {
          case Some(pair) => List(pair)
          case _          => 
            val charOpts = opt.tail.toSeq map ("-" + _)
            if (charOpts forall (envActionMap contains _))
              charOpts map envActionMap
            else Nil
        }
      }
    }
  }
}
