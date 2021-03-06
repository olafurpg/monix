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

import monix.eval.Coeval
import monix.reactive.Observable
import monix.reactive.exceptions.DummyException
import scala.concurrent.duration._
import scala.util.Failure

object FoldLeftObservableSuite extends BaseOperatorSuite {
  def createObservable(sourceCount: Int) = Some {
    val obs = Observable.range(0, sourceCount).foldLeftF(0L)(_+_)
    Sample(obs, 1, (sourceCount-1) * sourceCount / 2, 0.seconds, 0.seconds)
  }

  def observableInError(sourceCount: Int, ex: Throwable) = Some {
    val obs = Observable.range(0, sourceCount).endWithError(ex).foldLeftF(Coeval(0L))(_+_)
    Sample(obs, 0, 0, 0.seconds, 0.seconds)
  }

  def cancelableObservables() = {
    val obs = Observable.range(0, 1000).delaySubscription(1.seconds).foldLeftF(Coeval(0L))(_+_)
    Seq(Sample(obs, 0, 0, 0.seconds, 0.seconds))
  }

  def brokenUserCodeObservable(sourceCount: Int, ex: Throwable) = Some {
    val obs = Observable.range(0, sourceCount).endWithError(ex).foldLeftF(Coeval(0L))((_,_) => throw ex)
    Sample(obs, 0, 0, 0.seconds, 0.seconds)
  }

  test("should trigger error if the initial state triggers errors") { implicit s =>
    val ex = DummyException("dummy")
    val obs = Observable(1,2,3,4).foldLeftF[Int](Coeval.raiseError(ex))(_+_)
    val f = obs.runAsyncGetFirst; s.tick()
    assertEquals(f.value, Some(Failure(ex)))
  }
}
