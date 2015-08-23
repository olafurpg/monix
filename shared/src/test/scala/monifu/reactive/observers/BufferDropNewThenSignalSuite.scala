/*
 * Copyright (c) 2014-2015 by its authors. Some rights reserved.
 * See the project homepage at: http://www.monifu.org
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

package monifu.reactive.observers


import minitest.TestSuite
import monifu.concurrent.Scheduler
import monifu.concurrent.schedulers.TestScheduler
import monifu.reactive.Ack.{Cancel, Continue}
import monifu.reactive.OverflowStrategy.DropNew
import monifu.reactive.{Ack, DummyException, Observer}
import scala.concurrent.{Future, Promise}


object BufferDropNewThenSignalSuite extends TestSuite[TestScheduler] {
  def setup() = TestScheduler()
  def tearDown(s: TestScheduler) = {
    assert(s.state.get.tasks.isEmpty,
      "TestScheduler should have no pending tasks")
  }

  def buildNewForInt(bufferSize: Int, underlying: Observer[Int])
    (implicit s: Scheduler): BufferedSubscriber[Int] = {
    BufferedSubscriber(underlying, DropNew(bufferSize), nr => nr.toInt)
  }

  def buildNewForLong(bufferSize: Int, underlying: Observer[Long])
    (implicit s: Scheduler): BufferedSubscriber[Long] = {
    BufferedSubscriber(underlying, DropNew(bufferSize), nr => nr)
  }
  
  test("should not lose events, test 1") { implicit s =>
    var number = 0
    var wasCompleted = false

    val underlying = new Observer[Int] {
      def onNext(elem: Int): Future[Ack] = {
        number += 1
        Continue
      }

      def onError(ex: Throwable): Unit = {
        s.reportFailure(ex)
      }

      def onComplete(): Unit = {
        wasCompleted = true
      }
    }

    val buffer = buildNewForInt(1000, underlying)
    for (i <- 0 until 1000) buffer.observer.onNext(i)
    buffer.observer.onComplete()

    assert(!wasCompleted)
    s.tick()
    assert(number == 1000)
    assert(wasCompleted)
  }

  test("should not lose events, test 2") { implicit s =>
    var number = 0
    var completed = false

    val underlying = new Observer[Int] {
      def onNext(elem: Int): Future[Ack] = {
        number += 1
        Continue
      }

      def onError(ex: Throwable): Unit = {
        s.reportFailure(ex)
      }

      def onComplete(): Unit = {
        completed = true
      }
    }

    val buffer = buildNewForInt(1000, underlying)

    def loop(n: Int): Unit =
      if (n > 0)
        s.execute { buffer.observer.onNext(n); loop(n-1) }
      else
        buffer.observer.onComplete()

    loop(10000)
    assert(!completed)
    assertEquals(number, 0)

    s.tick()
    assert(completed)
    assertEquals(number, 10000)
  }

  test("should drop incoming when over capacity") { implicit s =>
    var received = 0
    var wasCompleted = false
    val promise = Promise[Ack]()

    val underlying = new Observer[Int] {
      def onNext(elem: Int) = {
        received += elem
        if (elem < 5) Continue else promise.future
      }

      def onError(ex: Throwable) = ()

      def onComplete() = {
        wasCompleted = true
      }
    }

    val buffer = buildNewForInt(5, underlying)

    assertEquals(buffer.observer.onNext(1), Continue)
    assertEquals(buffer.observer.onNext(2), Continue)
    assertEquals(buffer.observer.onNext(3), Continue)
    assertEquals(buffer.observer.onNext(4), Continue)
    assertEquals(buffer.observer.onNext(5), Continue)

    s.tick()
    assertEquals(received, 15)

    for (i <- 0 until 10)
      assertEquals(buffer.observer.onNext(6 + i), Continue)

    s.tick()
    assertEquals(received, 15)

    promise.success(Continue); s.tick()
    assertEquals(received, 15)

    for (i <- 0 until 4)
      assertEquals(buffer.observer.onNext(6 + i), Continue)

    s.tick()
    assertEquals(received, 55)

    buffer.observer.onComplete(); s.tick()
    assert(wasCompleted, "wasCompleted should be true")
  }

  test("should send onError when empty") { implicit s =>
    var errorThrown: Throwable = null
    val buffer = buildNewForInt(5, new Observer[Int] {
      def onError(ex: Throwable) = {
        errorThrown = ex
      }

      def onNext(elem: Int) = throw new IllegalStateException()
      def onComplete() = throw new IllegalStateException()
    })

    buffer.observer.onError(DummyException("dummy"))
    s.tickOne()

    assertEquals(errorThrown, DummyException("dummy"))
    val r = buffer.observer.onNext(1)
    assertEquals(r, Cancel)
  }

  test("should send onError when in flight") { implicit s =>
    var errorThrown: Throwable = null
    val buffer = buildNewForInt(5, new Observer[Int] {
      def onError(ex: Throwable) = {
        errorThrown = ex
      }
      def onNext(elem: Int) = Continue
      def onComplete() = throw new IllegalStateException()
    })

    buffer.observer.onNext(1)
    buffer.observer.onError(DummyException("dummy"))
    s.tickOne()

    assertEquals(errorThrown, DummyException("dummy"))
  }

  test("should send onError when at capacity") { implicit s =>
    var errorThrown: Throwable = null
    val promise = Promise[Ack]()

    val buffer = buildNewForInt(5, new Observer[Int] {
      def onError(ex: Throwable) = {
        errorThrown = ex
      }
      def onNext(elem: Int) = promise.future
      def onComplete() = throw new IllegalStateException()
    })

    buffer.observer.onNext(1)
    buffer.observer.onNext(2)
    buffer.observer.onNext(3)
    buffer.observer.onNext(4)
    buffer.observer.onNext(5)
    buffer.observer.onError(DummyException("dummy"))

    promise.success(Continue)
    s.tick()

    assertEquals(errorThrown, DummyException("dummy"))
  }

  test("should do onComplete only after all the queue was drained") { implicit s =>
    var sum = 0L
    var wasCompleted = false
    val startConsuming = Promise[Continue]()

    val buffer = buildNewForLong(10000, new Observer[Long] {
      def onNext(elem: Long) = {
        sum += elem
        startConsuming.future
      }
      def onError(ex: Throwable) = throw ex
      def onComplete() = wasCompleted = true
    })

    (0 until 9999).foreach(x => buffer.observer.onNext(x))
    buffer.observer.onComplete()
    startConsuming.success(Continue)

    s.tick()
    assert(wasCompleted)
    assert(sum == (0 until 9999).sum)
  }

  test("should do onComplete only after all the queue was drained, test2") { implicit s =>
    var sum = 0L
    var wasCompleted = false

    val buffer = buildNewForLong(10000, new Observer[Long] {
      def onNext(elem: Long) = {
        sum += elem
        Continue
      }
      def onError(ex: Throwable) = throw ex
      def onComplete() = wasCompleted = true
    })

    (0 until 9999).foreach(x => buffer.observer.onNext(x))
    buffer.observer.onComplete()
    s.tick()

    assert(wasCompleted)
    assert(sum == (0 until 9999).sum)
  }

  test("should do onError only after the queue was drained") { implicit s =>
    var sum = 0L
    var errorThrown: Throwable = null
    val startConsuming = Promise[Continue]()

    val buffer = buildNewForLong(10000, new Observer[Long] {
      def onNext(elem: Long) = {
        sum += elem
        startConsuming.future
      }
      def onError(ex: Throwable) = errorThrown = ex
      def onComplete() = throw new IllegalStateException()
    })

    (0 until 9999).foreach(x => buffer.observer.onNext(x))
    buffer.observer.onError(DummyException("dummy"))
    startConsuming.success(Continue)

    s.tick()
    assertEquals(errorThrown, DummyException("dummy"))
    assertEquals(sum, (0 until 9999).sum)
  }

  test("should do onError only after all the queue was drained, test2") { implicit s =>
    var sum = 0L
    var errorThrown: Throwable = null

    val buffer = buildNewForLong(10000, new Observer[Long] {
      def onNext(elem: Long) = {
        sum += elem
        Continue
      }
      def onError(ex: Throwable) = errorThrown = ex
      def onComplete() = throw new IllegalStateException()
    })

    (0 until 9999).foreach(x => buffer.observer.onNext(x))
    buffer.observer.onError(DummyException("dummy"))

    s.tick()
    assertEquals(errorThrown, DummyException("dummy"))
    assertEquals(sum, (0 until 9999).sum)
  }
}