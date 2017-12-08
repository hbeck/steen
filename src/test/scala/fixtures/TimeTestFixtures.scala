package fixtures

import core.Variable
import core.lars.TimePoint

/**
  * Created by FM on 16.05.16.
  */
trait TimeTestFixtures extends AtomTestFixture {
  val t0 = TimePoint(0)
  val t1 = TimePoint(1)
  val t2 = TimePoint(2)
  val t3 = TimePoint(3)
  val t4 = TimePoint(4)

  val U = Variable("U")
  val T = engine.asp.TimePinVariable
  val C = engine.asp.CountPinVariable
}
