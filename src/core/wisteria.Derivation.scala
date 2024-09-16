/*
    Wisteria, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package wisteria

import rudiments.*

import scala.deriving.*
import scala.compiletime.*

trait Derivation[TypeclassType[_]]
extends ProductDerivationMethods[TypeclassType], SumDerivationMethods[TypeclassType]:

  inline given [DerivationType](using Reflection[DerivationType])
      => TypeclassType[DerivationType] as derived =

    inline erasedValue[DerivationType] match
      case _: NamedTuple.NamedTuple[labelsType, tupleType] =>
        val reflection = summonInline[ProductReflection[tupleType]]

        type NamedTupleReflection = Mirror.ProductOf[NamedTuple.NamedTuple[labelsType, tupleType]]:
          type MirroredMonoType = NamedTuple.NamedTuple[labelsType, tupleType]
          type MirroredType = NamedTuple.NamedTuple[labelsType, tupleType]
          type MirroredLabel = reflection.MirroredLabel
          type MirroredElemTypes = tupleType
          type MirroredElemLabels = labelsType

        val reflection2 = reflection.asInstanceOf[NamedTupleReflection & Mirror.ProductOf[DerivationType & Product]]

        join[DerivationType & Product](using reflection2).asMatchable match
          case typeclass: TypeclassType[DerivationType] => typeclass

      case _ =>
        inline summon[Reflection[DerivationType]] match
          case reflection: ProductReflection[derivationType] =>
            join[derivationType](using reflection).asMatchable match
              case typeclass: TypeclassType[DerivationType] => typeclass

          case reflection: SumReflection[derivationType] =>
            split[derivationType](using reflection).asMatchable match
              case typeclass: TypeclassType[DerivationType] => typeclass
