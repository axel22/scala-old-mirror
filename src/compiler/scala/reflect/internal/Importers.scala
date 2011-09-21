package scala.reflect
package internal
import scala.collection.mutable.WeakHashMap

trait Importers { self: SymbolTable =>
  
  abstract class Importer {
    
    val from: SymbolTable
    
    lazy val symMap: WeakHashMap[from.Symbol, Symbol] = new WeakHashMap
    
    object reverse extends from.Importer {
      val from: self.type = self
      for ((fromsym, mysym) <- Importer.this.symMap) symMap += ((mysym, fromsym))
    }
    
    def importPosition(pos: from.Position): Position = NoPosition
    
    def importSymbol(sym: from.Symbol): Symbol = {
      def importOrRelink: Symbol = 
        if (sym == null)
          null
        else if (sym.getClass == NoSymbol.getClass)
          NoSymbol
        else if (sym.isRoot)
          definitions.RootClass
        else {
          val myowner = importSymbol(sym.owner)
          val mypos = importPosition(sym.pos)
          val myname = importName(sym.name)
 
          def doImport: Symbol = {
            val mysym = sym match {
              case x: from.MethodSymbol =>
                val mysym = new MethodSymbol(myowner, mypos, myname)
                mysym.referenced = importSymbol(x.referenced)
                mysym
              case x: from.ModuleSymbol =>
                new ModuleSymbol(myowner, mypos, myname)
              case x: from.TermSymbol =>
                new TermSymbol(myowner, mypos, myname)
              case x: from.TypeSkolem =>
                new TypeSkolem(myowner, mypos, myname.toTypeName, x.unpackLocation match {
                  case null => null
                  case y: from.Tree => importTree(y)
                  case y: from.Symbol => importSymbol(y)
                })
/*
              case x: from.ModuleClassSymbol =>
                val mysym = new ModuleClassSymbol(myowner, mypos, myname.toTypeName)
                mysym.sourceModule = importSymbol(x.sourceModule)
                mysym
*/
              case x: from.ClassSymbol =>
                val mysym = new ClassSymbol(myowner, mypos, myname.toTypeName)
                if (sym.thisSym != sym) {
                  mysym.typeOfThis = importType(sym.typeOfThis)
                  mysym.thisSym.name = importName(sym.thisSym.name)
                }
                mysym
              case x: from.TypeSymbol =>
                new TypeSymbol(myowner, mypos, myname.toTypeName)
            }
            symMap(sym) = mysym
            mysym setFlag sym.flags
            mysym setInfo {
              val mytypeParams = sym.typeParams map importSymbol
              new LazyPolyType(mytypeParams) {
                override def complete(s: Symbol) {
                  val result = sym.info match {
                    case from.PolyType(_, res) => res
                    case result => result
                  }
                  s setInfo polyType(mytypeParams, importType(result))
                  s setAnnotations (sym.annotations map importAnnotationInfo)
                }
              }
            }
          } // end doImport
          
          if (myowner.isClass && !myowner.isRefinementClass && sym.owner.info.decl(sym.name).exists) { 
            // symbol is in class scope, try to find equivalent one in local scope
            if (sym.isOverloaded)
              myowner.newOverloaded(myowner.thisType, sym.alternatives map importSymbol)
            else {
              var existing: Symbol = myowner.info.decl(myname)
              if (existing.isOverloaded) {
                existing = 
                  if (sym.isMethod) {
                    val localCopy = doImport
                    existing filter (_.tpe matches localCopy.tpe)
                  } else { 
                    existing filter (!_.isMethod)
                  }
                assert(!existing.isOverloaded)
              }
              if (existing.exists) existing
              else {
                val mysym = doImport
                myowner.info.decls enter mysym
                mysym
              }
            }
          } else if (sym.isTypeParameter && sym.paramPos >= 0)
            myowner.typeParams(sym.paramPos)
          else 
            doImport
        }
      symMap getOrElseUpdate (sym, importOrRelink)
    }

    def importType(tpe: from.Type): Type = tpe match {
      case x: from.TypeRef =>
        TypeRef(importType(x.pre), importSymbol(x.sym), x.args map importType)
      case x: from.ThisType =>
        ThisType(importSymbol(x.typeSymbol))
      case x: from.SingleType =>
        SingleType(importType(x.pre), importSymbol(x.sym))
      case x: from.MethodType =>
        MethodType(x.params map importSymbol, importType(x.resultType))
      case x: from.PolyType =>
        PolyType(x.typeParams map importSymbol, importType(x.resultType))
      case x: from.NullaryMethodType =>
        NullaryMethodType(importType(x.resultType))
      case x: from.ConstantType =>
        ConstantType(Constant(x.value.value))
      case x: from.SuperType =>
        SuperType(importType(x.thistpe), importType(x.supertpe))
      case x: from.TypeBounds =>
        TypeBounds(importType(x.lo), importType(x.hi))
      case x: from.BoundedWildcardType =>
        BoundedWildcardType(importTypeBounds(x.bounds))
      case x: from.ClassInfoType =>
        val myclazz = importSymbol(x.typeSymbol)
        val myscope = if (myclazz.isPackageClass) newPackageScope(myclazz) else newScope
        val myclazzTpe = ClassInfoType(x.parents map importType, myscope, myclazz)
        myclazz setInfo polyType(myclazz.typeParams, myclazzTpe) // needed so that newly created symbols find their scope
        for (sym <- x.decls) importSymbol(sym) // will enter itself into myclazz
        myclazzTpe
      case x: from.RefinedType =>
        RefinedType(x.parents map importType, importScope(x.decls), importSymbol(x.typeSymbol))
      case x: from.ExistentialType =>
        ExistentialType(x.typeParams map importSymbol, importType(x.resultType))
      case x: from.OverloadedType =>
        OverloadedType(importType(x.pre), x.alternatives map importSymbol)
      case x: from.AntiPolyType =>
        AntiPolyType(importType(x.pre), x.targs map importType)
      case x: from.TypeVar =>
        new TypeVar(importType(x.origin), importTypeConstraint(x.constr0), x.typeArgs map importType, x.params map importSymbol)
      case x: from.NotNullType =>
        NotNullType(importType(x.underlying))
      case x: from.AnnotatedType =>
        AnnotatedType(x.annotations map importAnnotationInfo, importType(x.underlying), importSymbol(x.selfsym))
      case x =>
        val xclazz = x.getClass
        if (xclazz == ErrorType.getClass) ErrorType
        else if (xclazz == WildcardType.getClass) WildcardType
        else if (xclazz == NoType.getClass) NoType
        else if (xclazz == NoPrefix.getClass) NoPrefix
        else if (x == null) null
        else throw new MatchError(x)
    }

    def importTypeBounds(bounds: from.TypeBounds) = importType(bounds).asInstanceOf[TypeBounds]

    def importAnnotationInfo(ann: from.AnnotationInfo): AnnotationInfo =
      AnnotationInfo(importType(ann.atp), ann.args map importTree, ann.assocs map {
        case (name, arg) => (importName(name), importAnnotArg(arg))
      })

    def importAnnotArg(arg: from.ClassfileAnnotArg): ClassfileAnnotArg = arg match {
      case x: from.LiteralAnnotArg =>
        LiteralAnnotArg(Constant(x.const.value))
      case x: from.ArrayAnnotArg =>
        ArrayAnnotArg(x.args map importAnnotArg)
      case x: from.ScalaSigBytes =>
        ScalaSigBytes(x.bytes)
      case x: from.NestedAnnotArg =>
        NestedAnnotArg(importAnnotationInfo(x.annInfo))
    }

    def importTypeConstraint(constr: from.TypeConstraint): TypeConstraint = {
      val result = new TypeConstraint(constr.loBounds map importType, constr.hiBounds map importType)
      result.inst = importType(constr.inst)
      result
    }

    // !!! todo: override to vcater for PackageScopes
    def importScope(decls: from.Scope): Scope =
      new Scope(decls.toList map importSymbol)

    def importName(name: from.Name): Name =
      if (name.isTypeName) newTypeName(name.toString) else newTermName(name.toString)
    def importTypeName(name: from.TypeName): TypeName = importName(name).toTypeName
    def importTermName(name: from.TermName): TermName = importName(name).toTermName

    def importModifiers(mods: from.Modifiers): Modifiers =
      new Modifiers(mods.flags, importName(mods.privateWithin), mods.annotations map importTree)

    def importImportSelector(sel: from.ImportSelector): ImportSelector =
      new ImportSelector(importName(sel.name), sel.namePos, importName(sel.rename), sel.renamePos)

    def importTree(tree: from.Tree): Tree = {
      tree match {
        case from.ClassDef(mods, name, tparams, impl) =>
          new ClassDef(importModifiers(mods), importName(name).toTypeName, tparams map importTypeDef, importTemplate(impl))
        case from.PackageDef(pid, stats) =>
          new PackageDef(importRefTree(pid), stats map importTree)
        case from.ModuleDef(mods, name, impl) =>
          new ModuleDef(importModifiers(mods), importName(name).toTermName, importTemplate(impl))
        case from.ValDef(mods, name, tpt, rhs) =>
          new ValDef(importModifiers(mods), importName(name).toTermName, importTree(tpt), importTree(rhs))
        case from.DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
          new DefDef(importModifiers(mods), importName(name).toTermName, tparams map importTypeDef, vparamss map (_ map importValDef), importTree(tpt), importTree(rhs))
        case from.TypeDef(mods, name, tparams, rhs) =>
          new TypeDef(importModifiers(mods), importName(name).toTypeName, tparams map importTypeDef, importTree(rhs))
        case from.LabelDef(name, params, rhs) =>
          new LabelDef(importName(name).toTermName, params map importIdent, importTree(rhs))
        case from.Import(expr, selectors) =>
          new Import(importTree(expr), selectors map importImportSelector)
        case from.Template(parents, self, body) =>
          new Template(parents map importTree, importValDef(self), body map importTree)
        case from.Block(stats, expr) =>
          new Block(stats map importTree, importTree(expr))
        case from.CaseDef(pat, guard, body) =>
          new CaseDef(importTree(pat), importTree(guard), importTree(body))
        case from.Alternative(trees) =>
          new Alternative(trees map importTree)
        case from.Star(elem) =>
          new Star(importTree(elem))
        case from.Bind(name, body) =>
          new Bind(importName(name), importTree(body))
        case from.UnApply(fun, args) =>
          new UnApply(importTree(fun), args map importTree)
        case from.ArrayValue(elemtpt ,elems) =>
          new ArrayValue(importTree(elemtpt), elems map importTree)
        case from.Function(vparams, body) =>
          new Function(vparams map importValDef, importTree(body))
        case from.Assign(lhs, rhs) =>
          new Assign(importTree(lhs), importTree(rhs))
        case from.If(cond, thenp, elsep) =>
          new If(importTree(cond), importTree(thenp), importTree(elsep))
        case from.Match(selector, cases) =>
          new Match(importTree(selector), cases map importCaseDef)
        case from.Return(expr) =>
          new Return(importTree(expr))
        case from.Try(block, catches, finalizer) =>
          new Try(importTree(block), catches map importCaseDef, importTree(finalizer))
        case from.Throw(expr) =>
          new Throw(importTree(expr))
        case from.New(tpt) =>
          new New(importTree(tpt))
        case from.Typed(expr, tpt) =>
          new Typed(importTree(expr), importTree(tpt))
        case from.TypeApply(fun, args) =>
          new TypeApply(importTree(fun), args map importTree)
        case from.Apply(fun, args) => tree match {
          case _: from.ApplyToImplicitArgs =>
            new ApplyToImplicitArgs(importTree(fun), args map importTree)
          case _: from.ApplyImplicitView =>
            new ApplyImplicitView(importTree(fun), args map importTree)
          case _ =>
            new Apply(importTree(fun), args map importTree)
        }
        case from.ApplyDynamic(qual, args) =>
          new ApplyDynamic(importTree(qual), args map importTree)
        case from.Super(qual, mix) =>
          new Super(importTree(qual), importTypeName(mix))
        case from.This(qual) =>
          new This(importName(qual).toTypeName)
        case from.Select(qual, name) =>
          new Select(importTree(qual), importName(name))
        case from.Ident(name) => tree match {
          case _: from.BackQuotedIdent =>
            new BackQuotedIdent(importName(name))
          case _ =>
            new Ident(importName(name))
        }
        case from.Literal(from.Constant(value)) =>
          new Literal(Constant(value))
        case from.TypeTree() =>
          new TypeTree()
        case from.Annotated(annot, arg) =>
          new Annotated(importTree(annot), importTree(arg))
        case from.SingletonTypeTree(ref) =>
          new SingletonTypeTree(importTree(ref))
        case from.SelectFromTypeTree(qual, name) =>
          new SelectFromTypeTree(importTree(qual), importName(name).toTypeName)
        case from.CompoundTypeTree(templ) =>
          new CompoundTypeTree(importTemplate(templ))
        case from.AppliedTypeTree(tpt, args) =>
          new AppliedTypeTree(importTree(tpt), args map importTree)
        case from.TypeBoundsTree(lo, hi) =>
          new TypeBoundsTree(importTree(lo), importTree(hi))
        case from.ExistentialTypeTree(tpt, whereClauses) =>
          new ExistentialTypeTree(importTree(tpt), whereClauses map importTree)
        case from.EmptyTree =>
          EmptyTree
        case null =>
          null
      }
    } // copyAttrs tree

    def importValDef(tree: from.ValDef): ValDef = importTree(tree).asInstanceOf[ValDef]
    def importTypeDef(tree: from.TypeDef): TypeDef = importTree(tree).asInstanceOf[TypeDef]
    def importTemplate(tree: from.Template): Template = importTree(tree).asInstanceOf[Template]
    def importRefTree(tree: from.RefTree): RefTree = importTree(tree).asInstanceOf[RefTree]
    def importIdent(tree: from.Ident): Ident = importTree(tree).asInstanceOf[Ident]
    def importCaseDef(tree: from.CaseDef): CaseDef = importTree(tree).asInstanceOf[CaseDef]
  }
}
