package lars

import core.lars.{At, LarsProgram, _}
import fixtures.AtomTestFixture
import org.scalatest.FlatSpec

/**
  * Created by FM on 01.05.16.
  */
class LarsSpec extends FlatSpec with AtomTestFixture {

  def W = WindowAtom

  def STW = TimeWindow

  val r1 = UserDefinedLarsRule(c, Set(WindowAtom(TimeWindow(3), Diamond, a), d), Set(b))
  val r2 = UserDefinedLarsRule(AtAtom(TimePoint(1), c), Set(W(STW(5), Box, b)), Set(W(STW(3), Diamond, a), W(STW(1), At(TimePoint(3)), a)))

  val rb1 = c <= WindowAtom(TimeWindow(3), Diamond, a) and d not (b)
  val rb2 = AtAtom(TimePoint(1), c) <= W(STW(5), Box, b) not W(STW(3), Diamond, a) not W(STW(1), At(TimePoint(3)), a)

  "A simple LARS program" should "be formatted as string" in {

    //val r = c :- WindowAtom(SlidingTimeWindow(3), Diamond, a) and not(b)
    //val r2 = c :- WindowAtom(SlidingTimeWindow(5), Box, b) and not(WindowAtom(SlidingTimeWindow(3), Diamond, a)) and not(WindowAtom(SlidingTimeWindow(1), At(Time(3)), a))

    val program = LarsProgram.from(r1, r2)

    val formatted = Format.symbolic(program)

    assert(formatted.size == 2)
    assert(formatted.head contains "c :-")
    assert(formatted.head contains "⊞")
    assert(formatted.head contains "◇")
    assert(formatted.head contains "not b")

    assert(formatted.last contains "@_1 c :-")
    assert(formatted.last contains "⊞_t^")
    assert(formatted.last contains "[5 SECONDS]")
    assert(formatted.last contains "☐ b")
    assert(formatted.last contains "not ⊞_t^")
    assert(formatted.last contains "@_3 a")
  }
}
