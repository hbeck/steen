package engine.asp

import core._
import core.asp.{AspFact, NormalRule}
import core.lars.TimePoint
import engine.DefaultTrackedSignal
import engine.asp.tms.{Pin, TickBasedAspToIncrementalAsp}


/**
  * Created by hb on 05.03.17.
  *
  * (This class does the pinning, grounding is done by the IncrementalEvaluationEngine)
  */
case class IncrementalRuleMaker(larsProgramEncoding: LarsProgramEncoding) {

  val R: Seq[(TicksUntilOutdated,NormalRule)] = larsProgramEncoding.larsRuleEncodings map { encoding =>
    val rule = TickBasedAspToIncrementalAsp.stripTickAtoms(encoding.aspRule)
    (encoding.ticksUntilOutdated,rule)
  }
  val Q: Seq[(TicksUntilOutdated,NormalRule)] = larsProgramEncoding.nowAndAtNowIdentityRules map { r =>
    (TickPair(1,Void),TickBasedAspToIncrementalAsp.stripTickAtoms(r))
  }

  val (nonExpiringR,expiringR) = R partition { case (ticks,_) => ticks.time == Void && ticks.count == Void }

  val (groundNonExpiringR,nonGroundNonExpiringR) = nonExpiringR partition { case (_,r) => r.isGround }

  val staticGroundRules = groundNonExpiringR map { case (_,r) => r } //TODO add those of window atom encoders later

  /*
   * TODO separate rules:
   * a) no tick-variables
   * b) only time-tick variables
   * c) only count-tick variables
   * d) both tick-variables
   */

  def rulesToGroundFor(currentTick: TickPair, signal: Option[Atom]): Seq[(Expiration,NormalRule)] = { //TODO naming tick, tickPair, now...
    val pinWithExp = timeCountPinned(currentTick) _
    val tickFact: NormalRule = AspFact(Atom(tickPredicate,Seq(TimePoint(currentTick.time),Value(currentTick.count.toInt))))
    val expTickFact: (Expiration, NormalRule) = (TickPair(-1,-1),tickFact)
    val facts:Seq[(Expiration,NormalRule)] = Seq(expTickFact) ++ {
      signal match {
        case Some(atom) => pinnedAtoms(DefaultTrackedSignal(atom,currentTick))
        case None => Seq()
      }
    }
    //note that this approach is not uniform. would be more elegant to return incremental rules with
    //tick variables plus TicksUntilOutdated, and uniformly pin them (like pin(Q) and pin(expiringR)
    val windowRules: Seq[(Expiration,NormalRule)] = larsProgramEncoding.windowAtomEncoders flatMap (_.incrementalRules(currentTick))
    val pq = pinWithExp(Q)
    val pr = pinWithExp(expiringR)
    val pw = pinWithExp(windowRules)
    facts ++ pq ++ pr ++ nonExpiringR ++ pw
  }

  def timeCountPinned(now: TickPair)(rules: Seq[(TicksUntilOutdated,NormalRule)]): Seq[(Expiration,NormalRule)] = {
    val pin = Pin(now.time,now.count)
    rules map {
      case (ticksUntilOutdated,rule) => (now+ticksUntilOutdated, pin.groundTickVariables(rule))
    }
  }

  /*
  def timePinned(time: Long)(rules: Seq[(TicksUntilOutdated,NormalRule)]) = timeCountPinned(TickPair(time,Void))(rules)

  def countPinned(count: Long)(rules: Seq[(TicksUntilOutdated,NormalRule)]) = timeCountPinned(TickPair(Void,count))(rules)
  */

  def pinnedAtoms(t: DefaultTrackedSignal): Seq[(Expiration,NormalRule)] = {
    // Seq(t.timePinned, t.countPinned, t.timeCountPinned) map {
    Seq(t.timePinned, t.timeCountPinned) map {
      a => (TickPair(Void,Void),AspFact[Atom](a)) //TODO reconsider outdating of signals
    }
  }

  //val signalTracker = new SignalTracker(larsProgramEncoding.maximumTimeWindowSizeInTicks, larsProgramEncoding.maximumTupleWindowSize, DefaultTrackedSignal.apply)

  //  def trackSignal(get: Atom) = {
  //    val trackedSignal = signalTracker.track(networkTime, signal)
  //  }

}
