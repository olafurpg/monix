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

import monix.streams._
import monix.execution.Scheduler

/**
  * A `BehaviorSubject` is a [[Subject]] that uses an underlying
  * [[broadcast.BehaviorPipe BehaviorPipe]].
  */
final class BehaviorSubject[T] private
  (initialValue: T, policy: OverflowStrategy.Synchronous, onOverflow: Long => T, s: Scheduler)
  extends PipedSubject[T,T](BehaviorPipe[T](initialValue), policy, onOverflow, s)

object BehaviorSubject {
  /**
    * Builds a [[Subject Subject]] that uses an underlying
    * [[broadcast.BehaviorPipe BehaviorPipe]].
    *
    * @param strategy - the [[OverflowStrategy overflow strategy]]
    *        used for buffering, which specifies what to do in case
    *        we're dealing with slow consumers: should an unbounded
    *        buffer be used, should back-pressure be applied, should
    *        the pipeline drop newer or older events, should it drop
    *        the whole buffer?  See [[OverflowStrategy]] for more
    *        details.
    */
  def apply[T](initial: T, strategy: OverflowStrategy.Synchronous)
    (implicit s: Scheduler): BehaviorSubject[T] =
    new BehaviorSubject[T](initial, strategy, null, s)

  /** Builds a [[Subject Subject]] that uses an underlying
    * [[broadcast.BehaviorPipe BehaviorPipe]].
    *
    * @param strategy   - the [[OverflowStrategy overflow strategy]]
    *                   used for buffering, which specifies what to do in case
    *                   we're dealing with slow consumers: should an unbounded
    *                   buffer be used, should back-pressure be applied, should
    *                   the pipeline drop newer or older events, should it drop
    *                   the whole buffer?  See [[OverflowStrategy]] for more
    *                   details.
    * @param onOverflow - a function that is used for signaling a special
    *                   event used to inform the consumers that an overflow event
    *                   happened, function that receives the number of dropped
    *                   events as a parameter (see [[OverflowStrategy.Evicted]])
    */
  def apply[T](initial: T, strategy: OverflowStrategy.Evicted, onOverflow: Long => T)
    (implicit s: Scheduler): BehaviorSubject[T] =
    new BehaviorSubject[T](initial, strategy, onOverflow, s)
}
