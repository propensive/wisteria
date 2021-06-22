/*

    Wisteria, version 2.0.0. Copyright 2018-21 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is
    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and limitations under the License.

*/
package wisteria

import scala.quoted.*

object Macro:
  inline def isObject[T]: Boolean = ${isObject[T]}
  inline def anns[T]: List[Any] = ${anns[T]}
  inline def typeAnns[T]: List[Any] = ${typeAnns[T]}
  inline def paramAnns[T]: List[(String, List[Any])] = ${paramAnns[T]}
  inline def isValueClass[T]: Boolean = ${isValueClass[T]}
  inline def defaultValue[T]: List[(String, Option[Any])] = ${defaultValue[T]}
  inline def paramTypeAnns[T]: List[(String, List[Any])] = ${paramTypeAnns[T]}
  inline def repeated[T]: List[(String, Boolean)] = ${repeated[T]}
  inline def typeInfo[T]: TypeInfo = ${typeInfo[T]}

  def isObject[T: Type](using Quotes): Expr[Boolean] =
    import quotes.reflect.*

    Expr(TypeRepr.of[T].typeSymbol.flags.is(Flags.Module))
  

  def paramAnns[T: Type](using Quotes): Expr[List[(String, List[Any])]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    Expr.ofList {
      tpe.typeSymbol.primaryConstructor.paramSymss.flatten.map { field =>
        Expr(field.name) -> field.annotations.filter { a =>
          a.tpe.typeSymbol.maybeOwner.isNoSymbol ||
            a.tpe.typeSymbol.owner.fullName != "scala.annotation.internal"
        }.map(_.asExpr.asInstanceOf[Expr[Any]])
      }.filter(_._2.nonEmpty).map { (name, anns) => Expr.ofTuple(name, Expr.ofList(anns)) }
    }

  def anns[T: Type](using Quotes): Expr[List[Any]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    
    Expr.ofList {
      tpe.typeSymbol.annotations.filter { a =>
        a.tpe.typeSymbol.maybeOwner.isNoSymbol || a.tpe.typeSymbol.owner.fullName != "scala.annotation.internal"
      }.map(_.asExpr.asInstanceOf[Expr[Any]])
    }
  
  def typeAnns[T: Type](using Quotes): Expr[List[Any]] =
    import quotes.reflect.*
    
    def getAnnotations(t: TypeRepr): List[Term] = t match
      case AnnotatedType(inner, ann) => ann :: getAnnotations(inner)
      case _                         => Nil
    
    val tpe = TypeRepr.of[T]

    tpe.typeSymbol.tree match
      case ClassDef(_, _, parents, _, _) =>
        Expr.ofList {
          parents.collect { case t: TypeTree => t.tpe }.flatMap(getAnnotations).filter { a =>
            a.tpe.typeSymbol.maybeOwner.isNoSymbol ||
                a.tpe.typeSymbol.owner.fullName != "scala.annotation.internal"
          }.map(_.asExpr.asInstanceOf[Expr[Any]])
        }

      case _ =>
        Expr.ofList(List())
  
  def isValueClass[T: Type](using Quotes): Expr[Boolean] =
    import quotes.reflect.*
    
    Expr(TypeRepr.of[T].baseClasses.contains(Symbol.classSymbol("scala.AnyVal")))
  
  def defaultValue[T: Type](using Quotes): Expr[List[(String, Option[Any])]] =
    import quotes.reflect._

    // TODO: Implement RHS
    Expr.ofList(TypeRepr.of[T].typeSymbol.caseFields.map { case s => Expr(s.name -> None) })

  def paramTypeAnns[T: Type](using Quotes): Expr[List[(String, List[Any])]] =
    import quotes.reflect._

    def getAnnotations(t: TypeRepr): List[Term] = t match
      case AnnotatedType(inner, ann) => ann :: getAnnotations(inner)
      case _                         => Nil

    Expr.ofList {
      TypeRepr.of[T].typeSymbol.caseFields.map { field =>
        val tpeRepr = field.tree match
          case v: ValDef => v.tpt.tpe
          case d: DefDef => d.returnTpt.tpe
        
        Expr(field.name) -> getAnnotations(tpeRepr).filter { a =>
          a.tpe.typeSymbol.maybeOwner.isNoSymbol ||
              a.tpe.typeSymbol.owner.fullName != "scala.annotation.internal"
          }.map(_.asExpr.asInstanceOf[Expr[Any]])
      }.filter(_._2.nonEmpty).map { (name, annots) => Expr.ofTuple(name, Expr.ofList(annots)) }
    }
  
  def repeated[T: Type](using Quotes): Expr[List[(String, Boolean)]] =
    import quotes.reflect.*
    
    def isRepeated[T](tpeRepr: TypeRepr): Boolean = tpeRepr match
      case a: AnnotatedType =>
        a.annotation.tpe match
          case tr: TypeRef => tr.name == "Repeated"
          case _           => false
      case _ => false

    val tr = TypeRepr.of[T]
    val symbol = tr.typeSymbol
    val constr = symbol.primaryConstructor.tree.asInstanceOf[DefDef]
    
    val areRepeated = constr.paramss.flatMap(_.params.flatMap {
        case ValDef(name, tpeTree, _) => Some(name -> isRepeated(tpeTree.tpe))
        case _                        => None
      }
    )
    
    Expr(areRepeated)

  def typeInfo[T: Type](using Quotes): Expr[TypeInfo] =
    import quotes.reflect._
    
    def normalizedName(s: Symbol): String = if s.flags.is(Flags.Module) then s.name.stripSuffix("$") else s.name
    def name(tpe: TypeRepr) : Expr[String] = Expr(normalizedName(tpe.typeSymbol))

    def ownerNameChain(sym: Symbol): List[String] =
      if sym.isNoSymbol || sym == defn.EmptyPackageClass || sym == defn.RootPackage || sym == defn.RootClass
      then Nil
      else ownerNameChain(sym.owner) :+ normalizedName(sym)

    def owner(tpe: TypeRepr): Expr[String] = Expr(ownerNameChain(tpe.typeSymbol.maybeOwner).mkString("."))

    def typeInfo(tpe: TypeRepr): Expr[TypeInfo] = tpe match
      case AppliedType(tpe, args) =>
        '{TypeInfo(${owner(tpe)}, ${name(tpe)}, ${Expr.ofList(args.map(typeInfo))})}
      case _ =>
        '{TypeInfo(${owner(tpe)}, ${name(tpe)}, Nil)}

    typeInfo(TypeRepr.of[T])