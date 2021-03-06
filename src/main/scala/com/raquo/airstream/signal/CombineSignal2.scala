package com.raquo.airstream.signal

import com.raquo.airstream.features.CombineMemoryObservable2

class CombineSignal2[A, B, O](
  override protected[this] val parent1: Signal[A],
  override protected[this] val parent2: Signal[B],
  override protected[this] val combinator: (A, B) => O
) extends Signal[O] with CombineMemoryObservable2[A, B, O] {

  override protected[airstream] val topoRank: Int = (parent1.topoRank max parent2.topoRank) + 1

  override protected[this] def initialValue: O = combinator(parent1.now(), parent2.now())
}
