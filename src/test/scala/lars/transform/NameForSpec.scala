package lars.transform

import java.util.concurrent.TimeUnit

import core.lars._
import reasoner.common.PlainLarsToAspMapper

import scala.concurrent.duration._

/**
  * Created by FM on 05.05.16.
  */
class NameForSpec extends TransformLarsSpec {

  def predicateFor(windowAtom: WindowAtom) = DefaultLarsToPinnedProgram.predicateFor(windowAtom).caption

  "The name for window-atom wˆ1 d a" should "be w_te_1_d_a" in {
    val window = WindowAtom(TimeWindow(1), Diamond, a)
    assert(predicateFor(window) == "w_te_1_d_a")
  }
  "The name for window-atom wˆ1 b a" should "be w_te_1_b_a" in {
    val window = WindowAtom(TimeWindow(1), Box, a)
    assert(predicateFor(window) == "w_te_1_b_a")
  }
  "The name for window-atom wˆ1 at_1 a" should "be w_te_1_at_1_a" in {
    val window = WindowAtom(TimeWindow(1), At(t1), a)
    assert(predicateFor(window) == "w_te_1_at_1_a")
  }
  "The name for window-atom wˆ1 at_2 a" should "be w_te_1_at_2_a" in {
    val window = WindowAtom(TimeWindow(1), At(t2), a)
    assert(predicateFor(window) == "w_te_1_at_2_a")
  }
  "The name for window-atom wˆ1 at_U a" should "be w_te_1_at_U_a" in {
    val U = TimeVariableWithOffset("U")
    val window = WindowAtom(TimeWindow(1), At(U), a)
    assert(predicateFor(window) == "w_te_1_at_U_a")
  }
  "The name for window-atom wˆ1 d b" should "be w_te_1_d_b" in {
    val window = WindowAtom(TimeWindow(1), Diamond, b)
    assert(predicateFor(window) == "w_te_1_d_b")
  }
  "The name for window-atom wˆ2 d b" should "be w_te_2_d_b" in {
    val window = WindowAtom(TimeWindow(2), Diamond, b)
    assert(predicateFor(window) == "w_te_2_d_b")
  }

  "An window atom wˆ2 d b(1)" should "have the name w_te_2_d_b" in {
    val window = WindowAtom(TimeWindow(2), Diamond, b("1"))
    assert(predicateFor(window) == "w_te_2_d_b")
  }

  "The name for window-atom w_#^2 d b" should "be w_tu_2_d_b" in {
    val window = WindowAtom(TupleWindow(2), Diamond, b)
    assert(predicateFor(window) == "w_tu_2_d_b")
  }

  "The name for window-atom w_^1s d b at an engine tick of 100ms" should "be w_te_10_d_b" in {
    val window = WindowAtom(TimeWindow(1), Diamond, b)
    val larsToPinnedProgram = PlainLarsToAspMapper(100 milliseconds)
    assert(larsToPinnedProgram.predicateFor(window).caption == "w_te_10_d_b")
  }
}
