/* NSC -- new Scala compiler
 * Copyright 2005-2011 LAMP/EPFL
 * @author  Paul Phillips
 */

package scala.tools.nsc
package backend

import io.AbstractFile
import util.JavaClassPath
import util.ClassPath.{ JavaContext, DefaultJavaContext }
import scala.tools.util.PathResolver

trait JavaPlatform extends Platform[AbstractFile] {
  import global._
  import definitions.{ BoxesRunTimeClass, getMember }
  
  lazy val classPath  = new PathResolver(settings).result
  def rootLoader      = new loaders.JavaPackageLoader(classPath)

  private def depAnalysisPhase =
    if (settings.make.isDefault) Nil
    else List(dependencyAnalysis)

  def platformPhases = List(
    flatten,    // get rid of inner classes
    liftcode,   // generate reified trees
    genJVM      // generate .class files
  ) ::: depAnalysisPhase
  
  lazy val externalEquals = getMember(BoxesRunTimeClass, nme.equals_)
  def externalEqualsNumNum = getMember(BoxesRunTimeClass, "equalsNumNum")
  def externalEqualsNumChar = getMember(BoxesRunTimeClass, "equalsNumChar")
  def externalEqualsNumObject = getMember(BoxesRunTimeClass, "equalsNumObject")
  
  /** We could get away with excluding BoxedBooleanClass for the
   *  purpose of equality testing since it need not compare equal
   *  to anything but other booleans, but it should be present in
   *  case this is put to other uses.
   */
  def isMaybeBoxed(sym: Symbol): Boolean = {
    import definitions._
    (sym == ObjectClass) ||
    (sym == JavaSerializableClass) ||
    (sym == ComparableClass) ||
    (sym isNonBottomSubClass BoxedNumberClass) ||
    (sym isNonBottomSubClass BoxedCharacterClass) ||
    (sym isNonBottomSubClass BoxedBooleanClass)
  }  
}
