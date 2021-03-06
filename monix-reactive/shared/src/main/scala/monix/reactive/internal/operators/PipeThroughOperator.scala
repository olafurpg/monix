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

package monix.reactive.internal.operators

import monix.reactive.Pipe
import monix.reactive.observables.ObservableLike
import ObservableLike._
import monix.reactive.observers.Subscriber

private[reactive] final class PipeThroughOperator[-A,+B](pipe: Pipe[A,B])
  extends Operator[A,B] {

  def apply(sb: Subscriber[B]): Subscriber[A] = {
    val (in,out) = pipe.unicast
    out.unsafeSubscribeFn(sb)
    Subscriber(in, sb.scheduler)
  }
}
