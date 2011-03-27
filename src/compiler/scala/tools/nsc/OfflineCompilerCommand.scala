/* NSC -- new Scala compiler
 * Copyright 2005-2011 LAMP/EPFL
 * @author  Martin Odersky
 */

package scala.tools.nsc

import settings.FscSettings
import io.Directory

/** A compiler command for the offline compiler.
 *
 * @author Martin Odersky and Lex Spoon
 */
class OfflineCompilerCommand(arguments: List[String], settings: FscSettings) extends CompilerCommand(arguments, settings) {
  import settings.currentDir
  def extraFscArgs = List(currentDir.name, currentDir.value)
  
  locally {
    // if -current-dir is unset, we're on the client and need to obtain it.
    if (currentDir.isDefault) {
      // Prefer env variable PWD to system property user.dir because the former
      // deals better with paths not rooted at / (filesystem mounts.)
      val baseDirectory = System.getenv("PWD") match {
        case null   => Directory.Current getOrElse Directory("/")
        case dir    => Directory(dir)
      }
      currentDir.value = baseDirectory.path
    }
    else {
      // Otherwise we're on the server and will use it to absolutize the paths.
      settings.absolutize(currentDir.value)
    }
  }

  override def cmdName = "fsc"  
  override def usageMsg = (
    createUsageMsg("where possible fsc", false, x => x.isStandard && settings.isFscSpecific(x.name)) +
    "\nStandard scalac options also available:\n  " +
    createUsageMsg(x => x.isStandard && !settings.isFscSpecific(x.name))
  )
}
