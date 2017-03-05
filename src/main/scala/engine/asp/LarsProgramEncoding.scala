package engine.asp

import core.Atom
import core.asp.{AspFact, NormalProgram, NormalRule}
import core.lars.{LarsBasedProgram, LarsRule, TimePoint}
import engine.asp.tms.TickBasedAspToIncrementalAsp

/**
  * Created by fm on 20/02/2017.
  */
//to derive window atom encoding
trait WindowAtomEncoder {
  val length: Long

  val allWindowRules: Seq[NormalRule] //one-shot/reactive clingo solving: e.g. for window^3 diamond all 4 rules

  @deprecated
  def incrementalRulesAt(tick: TickPosition): IncrementalRules

  //def pinnedIncrementalRules(prevPosition: TickPosition, currPosition: TickPosition): Seq[NormalRule]
}

case class TickPosition(time: TimePoint, count: Long)


trait TimeWindowEncoder extends WindowAtomEncoder

trait TupleWindowEncoder extends WindowAtomEncoder

/*
   E.g.   c(X) :- win2 \Diamond a(X), not win3 \Box b(X)  //larsRule
   ==>    c(X) :- w_2_d_a(X), w_3_b_b(X)   //ruleEncoding
          atoms w_2_d_a(X)  and  w_3_b_b(X) are called windowAtomEncodings and get their WindowAtomEncoder objects
 */
case class LarsRuleEncoding(larsRule: LarsRule, ruleEncodings: Set[NormalRule], windowAtomEncoders: Set[WindowAtomEncoder])

case class LarsProgramEncoding(larsRuleEncodings: Seq[LarsRuleEncoding], nowAndAtNowIdentityRules: Seq[NormalRule], backgroundData: Set[Atom]) extends NormalProgram with LarsBasedProgram {

  /*
   * incremental stuff
   */

  val windowAtomEncoders = larsRuleEncodings flatMap (_.windowAtomEncoders)

  val (groundRuleEncodings,nonGroundRuleEncodings) = (larsRuleEncodings flatMap (_.ruleEncodings)) partition (_.isGround)

  val groundRules = Seq[NormalRule]() ++ (backgroundData map (AspFact(_))) ++ groundRuleEncodings

  @deprecated
  def rulesToGround(prevPosition: TickPosition, currPosition: TickPosition): Seq[NormalRule] = {
//    val pin = Pin(currPosition.time)
//    val atNewEq: Seq[NormalRule] = nowAndAtNowIdentityRules map (pin.ground(_))
//    val incrementalRules = windowAtomEncoders.flatMap {
//      e => e.pinnedIncrementalRules(prevPosition,currPosition)
//    }
//    atNewEq ++ nonGroundRuleEncodings ++ incrementalRules
    //TODO
    Seq()
  }

  /*
   * one-shot stuff
   */

  //note that baseRules do not include the rules to derive the windowAtomEncodings
  val baseRules = (larsRuleEncodings flatMap (_.ruleEncodings)) ++ nowAndAtNowIdentityRules ++ (backgroundData map (AspFact(_))) //for one-shot solving

  val (groundBaseRules, nonGroundBaseRules) = baseRules.
    map(TickBasedAspToIncrementalAsp.stripTickAtoms).
    partition(_.isGround)

  val oneShotWindowRules = windowAtomEncoders flatMap (_.allWindowRules)

  /*
   * general stuff
   */

  // full representation of Lars-Program as asp
  override val rules = baseRules ++ oneShotWindowRules

  override val larsRules = larsRuleEncodings map (_.larsRule)

  val maximumTimeWindowSizeInTicks: Long = larsRuleEncodings.
    flatMap(_.windowAtomEncoders).
    collect {
      case t: TimeWindowEncoder => t.length
    } match {
    case Nil => 0
    case x => x.max
  }

}

@deprecated
case class IncrementalRules(toAdd: Seq[NormalRule], toRemove: Seq[NormalRule]) {
  def ++(other: IncrementalRules) = IncrementalRules(toAdd ++ other.toAdd, toRemove ++ other.toRemove)
}

