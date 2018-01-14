package com.raquo.laminar.experimental.airstream.core

import com.raquo.laminar.experimental.airstream.eventbus.EventBus
import com.raquo.laminar.experimental.airstream.eventstream.EventStream
import com.raquo.laminar.experimental.airstream.fixtures.{Calculation, Effect, TestableOwner}
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable

class SyncObservableSpec extends FunSpec with Matchers {

  it("sync and softSync fix diamond case glitch (combineWith)") {

    implicit val testOwner: TestableOwner = new TestableOwner
    val bus = new EventBus[Int]

    val tens = bus.events.map(_ * 10)
    val hundreds = tens.map(_ * 10)

    val tuples = hundreds.combineWith(tens)
    val syncTuples = tuples.sync()
    val softSyncTuples = tuples.softSync()

    val calculations = mutable.Buffer[Calculation[(Int, Int)]]()
    val effects = mutable.Buffer[Effect[(Int, Int)]]()

    tuples
      .map(Calculation.log("tuples", calculations))
      .foreach(effects += Effect("tuples", _))
    syncTuples
      .map(Calculation.log("syncTuples", calculations))
      .foreach(effects += Effect("syncTuples", _))
    softSyncTuples
      .map(Calculation.log("softSyncTuples", calculations))
      .foreach(effects += Effect("softSyncTuples", _))

    // ---

    // On first event the "tuples" stream appears to have no glitch because
    // combined stream needs events from both streams to emit a tuple.
    bus.writer.onNext(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("tuples", (100, 10)),
      Calculation("syncTuples", (100, 10)),
      Calculation("softSyncTuples", (100, 10))
    )
    calculations.clear()

    effects shouldEqual mutable.Buffer(
      Effect("tuples", (100, 10)),
      Effect("syncTuples", (100, 10)),
      Effect("softSyncTuples", (100, 10))
    )
    effects.clear()

    // ---

    // Subsequent events demonstrate the glitch – without sync, an extra event
    // with inconsistent state is fired
    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("tuples", (200, 10)),
      Calculation("tuples", (200, 20)),
      Calculation("syncTuples", (200, 20)),
      Calculation("softSyncTuples", (200, 20))
    )
    calculations.clear()

    effects shouldEqual mutable.Buffer(
      Effect("tuples", (200, 10)), // 200 is updated first due to depth-first propagation
      Effect("tuples", (200, 20)),
      Effect("syncTuples", (200, 20)),
      Effect("softSyncTuples", (200, 20))
    )
    effects.clear()

    // ---

    bus.writer.onNext(3)

    calculations shouldEqual mutable.Buffer(
      Calculation("tuples", (300, 20)),
      Calculation("tuples", (300, 30)),
      Calculation("syncTuples", (300, 30)),
      Calculation("softSyncTuples", (300, 30))
    )
    calculations.clear()

    effects shouldEqual mutable.Buffer(
      Effect("tuples", (300, 20)),
      Effect("tuples", (300, 30)),
      Effect("syncTuples", (300, 30)),
      Effect("softSyncTuples", (300, 30))
    )
    effects.clear()
  }

  it("sync and softSync fix merge stream glitch") {

    implicit val testOwner: TestableOwner = new TestableOwner
    val bus = new EventBus[Int]
    val unrelatedBus = new EventBus[Int]

    val tens = bus.events.map(_ * 10)
    val hundreds = tens.map(_ * 10)

    val numbers = EventStream.merge(tens, hundreds, unrelatedBus.events)
    val syncNumbers = numbers.sync()
    val softSyncNumbers = numbers.softSync()

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[Effect[Int]]()

    numbers
      .map(Calculation.log("numbers", calculations))
      .foreach(effects += Effect("numbers", _))
    syncNumbers
      .map(Calculation.log("syncNumbers", calculations))
      .foreach(effects += Effect("syncNumbers", _))
    softSyncNumbers
      .map(Calculation.log("softSyncNumbers", calculations))
      .foreach(effects += Effect("softSyncNumbers", _))

    // Sync and Soft Sync should propagate only the last event
    // fired by the merged stream within the propagation

    // ---

    bus.writer.onNext(1)

    calculations shouldEqual mutable.Buffer(
      Calculation("numbers", 10),
      Calculation("numbers", 100),
      Calculation("syncNumbers", 100),
      Calculation("softSyncNumbers", 100)
    )
    calculations.clear()

    effects shouldEqual mutable.Buffer(
      Effect("numbers", 10),
      Effect("numbers", 100),
      Effect("syncNumbers", 100),
      Effect("softSyncNumbers", 100)
    )
    effects.clear()

    // ---

    // Firing an event on an unrelated bus should behave normally
    unrelatedBus.writer.onNext(-1)

    calculations shouldEqual mutable.Buffer(
      Calculation("numbers", -1),
      Calculation("syncNumbers", -1),
      Calculation("softSyncNumbers", -1)
    )
    calculations.clear()

    effects shouldEqual mutable.Buffer(
      Effect("numbers", -1),
      Effect("syncNumbers", -1),
      Effect("softSyncNumbers", -1)
    )
    effects.clear()

    // ---

    bus.writer.onNext(2)

    calculations shouldEqual mutable.Buffer(
      Calculation("numbers", 20),
      Calculation("numbers", 200),
      Calculation("syncNumbers", 200),
      Calculation("softSyncNumbers", 200)
    )
    calculations.clear()

    effects shouldEqual mutable.Buffer(
      Effect("numbers", 20),
      Effect("numbers", 200),
      Effect("syncNumbers", 200),
      Effect("softSyncNumbers", 200)
    )
    effects.clear()
  }

  ignore("dependent but not deadlocked pending observables resolve correctly") {

  }

  ignore("deadlocked pending observables resolve by firing a soft synced observable") {

  }

}