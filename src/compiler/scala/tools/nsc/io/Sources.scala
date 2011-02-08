package scala.tools.nsc
package io

import util.ClassPath
import java.util.concurrent.{ Future, ConcurrentHashMap, ExecutionException }
import java.util.zip.ZipException
import Path.{ isJarOrZip, locateJarByName }
import collection.JavaConverters._
import Properties.{ envOrElse, propOrElse }

class Sources(val path: String) {
  val expandedPath        = ClassPath.join(ClassPath expandPath path: _*)
  val cache               = new ConcurrentHashMap[String, List[Fileish]]
  def allNames            = cache.keys.asScala.toList.sorted
  def apply(name: String) = get(name)
  def size                = cache.asScala.values map (_.length) sum

  private var debug = false
  private def dbg(msg: => Any) = if (debug) Console println msg
  private val partitioned = ClassPath toPaths expandedPath partition (_.isDirectory)

  val dirs   = partitioned._1 map (_.toDirectory)
  val jars   = partitioned._2 filter isJarOrZip map (_.toFile)
  val (isDone, force) = {
    val f1  = spawn(calculateDirs())
    val f2  = spawn(calculateJars())    
    val fn1 = () => { f1.isDone() && f2.isDone() }
    val fn2 = () => { f1.get() ; f2.get() ; () }

    (fn1, fn2)
  }
  
  private def catchZip(body: => Unit): Unit = {
    try body
    catch { case x: ZipException => dbg("Caught: " + x) }
  }

  private def calculateDirs() =
    dirs foreach { d => dbg(d) ; catchZip(addSources(d.deepFiles map (x => Fileish(x)))) }

  private def calculateJars() = 
    jars foreach { j => dbg(j) ; catchZip(addSources(new SourceJar(j).iterator)) }
  
  private def addSources(fs: TraversableOnce[Fileish]) =
    fs foreach { f => if (f.isSourceFile) add(f.name, f) }

  private def get(key: String): List[Fileish] =
    if (cache containsKey key) cache.get(key) else Nil

  private def add(key: String, value: Fileish) = {
    if (cache containsKey key) cache.replace(key, value :: cache.get(key))
    else cache.put(key, List(value))
  }
  override def toString = "Sources(%d dirs, %d jars, %d sources)".format(
    dirs.size, jars.size, cache.asScala.values map (_.length) sum
  )
}

trait LowPrioritySourcesImplicits {
  self: Sources.type =>

  implicit def fallbackSources: Sources = defaultSources
}


object Sources extends LowPrioritySourcesImplicits {
  val scalaSourceJars     = List("scala-library-src.jar", "scala-compiler-src.jar")
  val sourcePathEnv       = envOrElse("SOURCEPATH", "")
  val scalaLibraryJarPath = (scalaSourceJars map locateJarByName).flatten map (_.path)
  val defaultSources      = apply(scalaLibraryJarPath :+ sourcePathEnv: _*)
    
  def apply(paths: String*): Sources = new Sources(ClassPath.join(paths: _*))
}
