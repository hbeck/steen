package lars.transform

import core.Atom
import core.asp.AspRule
import core.lars._
import engine.asp.LarsToPinnedProgram
import org.scalatest.Matchers._

/**
  * Created by FM on 07.05.16.
  */
class RuleSpec extends TransformLarsSpec {
  "A fact" should "be transformed into two rules" in {
    val f = LarsFact(a)

    DefaultLarsToPinnedProgram(f) should have size (1)
  }

  it should "contain the fact a(T)." in {
    val f = LarsFact(a)

    DefaultLarsToPinnedProgram(f) should contain(AspRule(a(T), Set(now(T))))
  }

  "A rule containing a window atom wˆ1 b a" should "be transformed into 2 rules" in {
    val r = UserDefinedLarsRule(b, Set(WindowAtom(SlidingTimeWindow(1), Box, a)))

    DefaultLarsToPinnedProgram(r) should have size 2
  }
  it should "contain a rule with head w_1_b_a(T)" in {
    val r = UserDefinedLarsRule(b, Set(WindowAtom(SlidingTimeWindow(1), Box, a)))

    DefaultLarsToPinnedProgram(r).map(_.head) should contain(Atom("w_te_1_b_a")(T))
  }
  it should "contain a rule with head w_1_b_a(T) mapped from neg." in {
    val r = UserDefinedLarsRule(b, Set(), Set(WindowAtom(SlidingTimeWindow(1), Box, a)))

    DefaultLarsToPinnedProgram(r).map(_.head) should contain(Atom("w_te_1_b_a")(T))
  }

  "A rule containing a window atom aˆ1 d a" should "be transformed into 3 rules" in {
    val r = UserDefinedLarsRule(b, Set(WindowAtom(SlidingTimeWindow(1), Diamond, a)))

    DefaultLarsToPinnedProgram(r) should have size 3
  }

  "A rule containing a window atom aˆ1 at_1 a" should "be transformed into 3 rules" in {
    val r = UserDefinedLarsRule(b, Set(WindowAtom(SlidingTimeWindow(1), At(t1), a)))

    DefaultLarsToPinnedProgram(r) should have size 3
  }

  "A rule containing a window atom aˆ0 d a" should "be transformed into 2 rules" in {
    val r = UserDefinedLarsRule(b, Set(WindowAtom(SlidingTimeWindow(0), Diamond, a)))

    DefaultLarsToPinnedProgram(r) should have size 2
  }

  "A rule containing a window atom wˆf d a" should "be transformed into 2 rules" in {
    val r = UserDefinedLarsRule(b, Set(WindowAtom(FluentWindow, Diamond, a)))

    DefaultLarsToPinnedProgram(r) should have size 2
  }
}
