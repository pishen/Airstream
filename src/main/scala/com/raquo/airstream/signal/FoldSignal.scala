package com.raquo.airstream.signal

import com.raquo.airstream.core.{Observable, Transaction}
import com.raquo.airstream.features.SingleParentObservable

class FoldSignal[A, B](
  override protected[this] val parent: Observable[A],
  makeInitialValue: () => B,
  fn: (B, A) => B
) extends Signal[B] with SingleParentObservable[A, B] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  override protected[airstream] def onNext(nextValue: A, transaction: Transaction): Unit = {
    fire(fn(now(), nextValue), transaction)
  }

  override protected[this] def initialValue: B = makeInitialValue()
}
