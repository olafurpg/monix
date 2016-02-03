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

import monix.streams.observers.SyncObserver

import scala.language.reflectiveCalls
import monix.streams.observables.LiftOperators
import monix.streams.{Ack, Observable, Subscriber}

/** A subject is meant for imperative style feeding of events.
  *
  * When emitting events, one doesn't need to follow the back-pressure contract.
  * On the other hand the grammar must still be respected:
  *
  *     (onNext)* (onComplete | onError)
  */
trait Subject[I,+O] extends SyncObserver[I] with Pipe[I,O]
  with LiftOperators[O, ({type λ[+α] = Subject[I, α]})#λ] { self =>

  protected override
  def liftToSelf[U](f: (Observable[O]) => Observable[U]): Subject[I, U] =
    new Subject[I, U] {
      def onNext(elem: I): Ack = self.onNext(elem)
      def onComplete(): Unit = self.onComplete()
      def onError(ex: Throwable): Unit = self.onError(ex)

      private[this] val lifted = f(self)
      def unsafeSubscribeFn(subscriber: Subscriber[U]): Unit =
        lifted.unsafeSubscribeFn(subscriber)
    }
}
