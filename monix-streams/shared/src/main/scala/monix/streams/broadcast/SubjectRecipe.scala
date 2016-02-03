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

package monix.streams.broadcast

import monix.execution.Scheduler
import monix.streams.OverflowStrategy.{Evicted, Synchronous}

/** Represents a [[Subject]] factory.
  *
  * Needed because Subjects are inherently side-effecting, stateful and have to
  * obey the `Observer` contract (so can't be reused for subscribing to
  * multiple observables).
  */
trait SubjectRecipe[I,+O] {
  /** Returns a new instance of a [[Subject]].
    *
    * @param overflowStrategy is a [[monix.streams.OverflowStrategy.Synchronous Synchronous]] overflow
    *        strategy that will be applied for asynchronous subscribers.
    */
  def newInstance(overflowStrategy: Synchronous)(implicit s: Scheduler): Subject[I,O]

  /** Returns a new instance of a [[Subject]].
    *
    * @param overflowStrategy is a [[monix.streams.OverflowStrategy.Evicted Evicted]] overflow
    *        strategy that will be applied for asynchronous subscribers.
    */
  def newInstance(overflowStrategy: Evicted, onOverflow: Long => I)(implicit s: Scheduler): Subject[I,O]
}

object SubjectRecipe {
  /** Subject recipe for building [[PublishSubject]] instances. */
  def publish[T](): SubjectRecipe[T,T] =
    new SubjectRecipe[T,T] {
      def newInstance(os: Synchronous)(implicit s: Scheduler): Subject[T,T] =
        PublishSubject[T](os)(s)

      def newInstance(os: Evicted, f: Long => T)(implicit s: Scheduler): Subject[T,T] =
        PublishSubject[T](os, f)(s)
    }

  /** Subject recipe for building [[BehaviorSubject]] instances. */
  def behavior[T](initial: T): SubjectRecipe[T,T] =
    new SubjectRecipe[T,T] {
      def newInstance(os: Synchronous)(implicit s: Scheduler): Subject[T,T] =
        BehaviorSubject[T](initial, os)(s)

      def newInstance(os: Evicted, f: Long => T)(implicit s: Scheduler): Subject[T,T] =
        BehaviorSubject[T](initial, os, f)(s)
    }

  /** Subject recipe for building [[AsyncSubject]] instances. */
  def async[T](): SubjectRecipe[T,T] =
    new SubjectRecipe[T,T] {
      def newInstance(os: Synchronous)(implicit s: Scheduler): Subject[T,T] =
        AsyncSubject[T](os)(s)

      def newInstance(os: Evicted, f: Long => T)(implicit s: Scheduler): Subject[T,T] =
        AsyncSubject[T](os, f)(s)
    }

  /** Subject recipe for building [[ReplaySubject]] instances. */
  def replay[T](): SubjectRecipe[T,T] =
    new SubjectRecipe[T,T] {
      def newInstance(os: Synchronous)(implicit s: Scheduler): Subject[T,T] =
        ReplaySubject[T](os)(s)

      def newInstance(os: Evicted, f: Long => T)(implicit s: Scheduler): Subject[T,T] =
        ReplaySubject[T](os, f)(s)
    }
}