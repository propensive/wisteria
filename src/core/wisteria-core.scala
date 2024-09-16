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

import scala.deriving.*

type Reflection[DerivationType] = Mirror.Of[DerivationType]
type ProductReflection[DerivationType <: Product] = Mirror.ProductOf[DerivationType]
type SumReflection[DerivationType] = Mirror.SumOf[DerivationType]

type Derivable[DerivationType <: { type Self }] =
  Derivation[[SelfType] =>> DerivationType { type Self = SelfType }]

type ProductDerivable[DerivationType <: { type Self }] =
  ProductDerivation[[SelfType] =>> DerivationType { type Self = SelfType }]

package derivationFeatures:
  given [NamesType <: Tuple, TupleType <: Tuple](using mirror: Mirror.ProductOf[TupleType])
      => Mirror.ProductOf[NamedTuple.NamedTuple[NamesType, TupleType]] as namedTupleDerivation:

    type MirroredMonoType = NamedTuple.NamedTuple[NamesType, TupleType]
    type MirroredType = MirroredMonoType
    type MirroredLabel = mirror.MirroredLabel
    type MirroredElemTypes = TupleType
    type MirroredElemLabels = NamesType

    def fromProduct(p: Product): MirroredMonoType =
      mirror.fromProduct(p).asInstanceOf[MirroredMonoType]
