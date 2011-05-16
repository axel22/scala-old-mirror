package scala.reflect
package common

import settings.MutableSettings

trait Required { self: SymbolTable =>
  
  type AbstractFile >: Null <: { def path: String }
  
  def picklerPhase: Phase
  
  val treePrinter: TreePrinter
  
  val gen: TreeGen { val global: Required.this.type }
  
  def settings: MutableSettings
}
