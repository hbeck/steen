package experimental.eval

import core.lars.{Diamond, LarsProgram, _}
import core.not
import fixtures._
import org.scalatest.Inspectors._
import org.scalatest.Matchers._
import org.scalatest.OptionValues._


/**
  * Created by FM on 02.06.16.
  */
class JtmsEssenceSimpleWindowSample extends ConfigurableReasonerSpec with TimeTestFixtures with JtmsIncrementalReasoner {
  val program = LarsProgram.from(
    a <= W(1, Diamond, c),
    c <= W(1, Diamond, a),
    b <= not(W(1, Diamond, a)),
    d <= W(1, Diamond, b),
    d <= W(1, Diamond, c)
  )


  "An empty data-stream" should "lead to model b,d" in {
    reasoner.evaluate(t0).get.value should contain allOf(b, d)
  }
  "A data-stream with {1 -> a}" should "lead to (a, c, d)" in {
    reasoner.append(t1)(a)

    reasoner.evaluate(t1).get.value should contain allOf(a, c, d)
    reasoner.evaluate(t2).get.value should contain allOf(a, c, d)
  }

  "A stream with alternating 'a' inputs" should "lead to (a, c, d) at all time points" in pendingWithJtms("cycle between a <-> c"){
    (1 to 100 by 2) foreach (reasoner.append(_)(a))

    assume(Set(b, d).subsetOf(reasoner.evaluate(t0).get.value))
    assume(Set(a, c, d).subsetOf(reasoner.evaluate(t1).get.value))

    forAll(1 to 100) {
      i => reasoner.evaluate(i).get.value should contain allOf(a, c, d)
    }
  }

}

class JtmsEssenceTests extends  RunWithAllImplementations(new JtmsEssenceSimpleWindowSample)