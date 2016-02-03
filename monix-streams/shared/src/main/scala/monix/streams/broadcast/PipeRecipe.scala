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

/** Represents a [[Pipe]] factory.
  *
  * Needed because Pipes are inherently side-effecting, stateful and have to
  * obey the `Observer` contract (so can't be reused for subscribing to
  * multiple observables).
  */
trait PipeRecipe[I,+O] {
  /** Returns a new instance of a [[Pipe]]. */
  def newInstance(): Pipe[I,O]
}

object PipeRecipe {
  /** Pipe recipe for building [[PublishPipe]] instances. */
  def publish[T](): PipeRecipe[T,T] =
    new PipeRecipe[T,T] {
      def newInstance(): Pipe[T,T] =
        PublishPipe[T]()
    }

  /** Pipe recipe for building [[BehaviorPipe]] instances. */
  def behavior[T](initial: => T): PipeRecipe[T,T] =
    new PipeRecipe[T,T] {
      def newInstance(): Pipe[T,T] =
        BehaviorPipe[T](initial)
    }

  /** Pipe recipe for building [[AsyncPipe]] instances. */
  def async[T](): PipeRecipe[T,T] =
    new PipeRecipe[T,T] {
      def newInstance(): Pipe[T,T] =
        AsyncPipe[T]()
    }

  /** Pipe recipe for building unbounded [[ReplayPipe]] instances. */
  def replay[T](): PipeRecipe[T,T] =
    new PipeRecipe[T,T] {
      def newInstance(): Pipe[T,T] =
        ReplayPipe[T]()
    }

  /** Pipe recipe for building unbounded [[ReplayPipe]] instances. */
  def replayPopulated[T](initial: => Seq[T]): PipeRecipe[T,T] =
    new PipeRecipe[T,T] {
      def newInstance(): Pipe[T,T] =
        ReplayPipe[T](initial:_*)
    }

  /** Pipe recipe for building [[ReplayPipe]] instances
    * with a maximum `capacity` (after which old items start being dropped)
    *
    * NOTE: the `capacity` is actually grown to the next power of 2 (minus 1),
    * because buffers sized as powers of two can be more efficient and the
    * underlying implementation is most likely to be a ring buffer. So give it
    * `300` and its capacity is going to be `512 - 1`
    */
  def replaySized[T](capacity: Int): PipeRecipe[T,T] =
    new PipeRecipe[T,T] {
      def newInstance(): Pipe[T,T] =
        ReplayPipe.createWithSize[T](capacity)
    }
}