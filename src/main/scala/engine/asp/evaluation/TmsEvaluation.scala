package engine.asp.evaluation

import core._
import core.lars.TimePoint
import engine.asp.{MappedProgram, PinnedAspToIncrementalAsp}
import jtms.ExtendedJtms

/**
  * Created by FM on 18.05.16.
  */
case class TmsEvaluation(pinnedAspProgram: MappedProgram, extendedJtms: ExtendedJtms = ExtendedJtms()) extends StreamingAspInterpreter {
  val incrementalProgram = PinnedAspToIncrementalAsp(pinnedAspProgram)

  val (groundRules, nonGroundRules) = incrementalProgram.rules partition (_.isGround)

  val tms = {
    groundRules foreach extendedJtms.add
    extendedJtms
  }

  def apply(timePoint: TimePoint, pinnedStream: PinnedStream): Option[PinnedModel] = {
    val pin = Pin(timePoint)

    val groundedRules = pin.ground(nonGroundRules)
    val groundedStream = pin.ground(pinnedStream) //TODO pinnedStream map (_.toNormal) oder so

    groundedRules foreach tms.add
    groundedStream foreach tms.add

    val resultingModel = tms.getModel() match {
      case Some(model) => Some(asPinnedAtoms(model, timePoint))
      case None => None
    }

    groundedRules foreach tms.remove
    groundedStream foreach tms.remove

    resultingModel
  }

  def asPinnedAtoms(model: Model, timePoint: TimePoint) = model map {
    case p: PinnedAtom => p

    // in incremental mode we assume that all (resulting) atoms are meant to be at T
    case a: Atom => a(timePoint)

    //    case a: Atom => throw new IllegalArgumentException(f"The atom $a is an invalid result (it cannot be converted into a PinnedAtom)")
  }

}
