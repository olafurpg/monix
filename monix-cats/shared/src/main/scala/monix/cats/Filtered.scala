/*
 * Copyright (c) 2014-2016 by its authors. Some rights reserved.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.cats

import _root_.cats.Functor
import simulacrum.typeclass
import scala.language.{higherKinds, implicitConversions}

/** A type-class representing data-structures that can be `empty`,
  * along with a filter operation that filters out `A` elements
  * based on the given predicate.
  */
@typeclass trait Filtered[F[_]] extends Functor[F] {
  /** Returns an empty `F` */
  def empty[A]: F[A]

  /** Filters the source for elements satisfying the given predicate. */
  def filter[A](fa: F[A])(p: A => Boolean): F[A]

  /** Given a partial function, filters and transforms the source by it. */
  def collect[A,B](fa: F[A])(pf: PartialFunction[A,B]): F[B] =
    map(filter(fa)(pf.isDefinedAt))(a => pf(a))

  /** Builds an empty instance that completes when the source completes. */
  def completed[A](fa: F[A]): F[A] =
    filter(fa)(a => false)

  /** Alias for [[completed]]. */
  final def ignoreElements[A](fa: F[A]): F[A] =
    completed(fa)
}
