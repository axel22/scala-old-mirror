/* NSC -- new Scala compiler
 * Copyright 2005-2011 LAMP/EPFL
 * @author  Paul Phillips
 */

package scala.reflect
package internal

trait TypeDebugging {
  self: SymbolTable =>
  
  import definitions._

  // @M toString that is safe during debugging (does not normalize, ...)
  object TypeDebugStrings {
    object str {
      def parentheses(xs: List[_]): String     = xs.mkString("(", ", ", ")")
      def brackets(xs: List[_]): String        = if (xs.isEmpty) "" else xs.mkString("[", ", ", "]")
      def tparams(tparams: List[Type]): String = brackets(tparams map debug)
      def parents(ps: List[Type]): String      = (ps map debug).mkString(" with ")
      def refine(defs: Scope): String          = defs.toList.mkString("{", " ;\n ", "}")      
    }
    
    def dump(tp: Type): Unit = {
      println("** " + tp + " / " + tp.getClass + " **")
      import tp._

      println("typeSymbol = " + typeSymbol)
      println("termSymbol = " + termSymbol)
      println("widen = " + widen)
      println("deconst = " + deconst)
      println("typeOfThis = " + typeOfThis)
      println("bounds = " + bounds)
      println("parents = " + parents)
      println("prefixChain = " + prefixChain)
      println("typeConstructor = " + typeConstructor)
      println(" .. typeConstructor.typeParams = " + typeConstructor.typeParams)
      println(" .. _.variance = " + (typeConstructor.typeParams map (_.variance)))
      println("typeArgs = " + typeArgs)
      println("resultType = " + resultType)
      println("finalResultType = " + finalResultType)
      println("paramss = " + paramss)
      println("paramTypes = " + paramTypes)
      println("typeParams = " + typeParams)
      println("boundSyms = " + boundSyms)
      println("baseTypeSeq = " + baseTypeSeq)
      println("baseClasses = " + baseClasses)
      println("toLongString = " + toLongString)
    }
    
    private def debug(tp: Type): String = tp match {
      case TypeRef(pre, sym, args)             => debug(pre) + "." + sym.nameString + str.tparams(args)
      case ThisType(sym)                       => sym.nameString + ".this"
      case SingleType(pre, sym)                => debug(pre) +"."+ sym.nameString +".type"
      case RefinedType(parents, defs)          => str.parents(parents) + str.refine(defs)
      case ClassInfoType(parents, defs, clazz) => "class "+ clazz.nameString + str.parents(parents) + str.refine(defs)
      case PolyType(tparams, result)           => str.brackets(tparams) + " " + debug(result)
      case TypeBounds(lo, hi)                  => ">: "+ debug(lo) +" <: "+ debug(hi)
      case tv @ TypeVar(_, _)                  => tv.toString
      case ExistentialType(tparams, qtpe)      => "forSome "+ str.brackets(tparams) + " " + debug(qtpe)
      case _                                   => tp.toString      
    }
    def debugString(tp: Type) = debug(tp)
  }
  private def TDS = TypeDebugStrings

  def paramString(tp: Type)      = TDS.str parentheses (tp.params map (_.defString))
  def typeParamsString(tp: Type) = TDS.str brackets (tp.typeParams map (_.defString))
  def typeArgsString(tp: Type)   = TDS.str brackets (tp.typeArgs map (_.safeToString))
  def debugString(tp: Type)      = TDS debugString tp
}

