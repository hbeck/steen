package engine.asp

import core.asp._
import core.lars.ExtendedAtom
import core.{Atom, PinnedAtom}
import engine.asp.evaluation.PinnedRule

/**
  * Created by FM on 08.06.16.
  *
  * Remove temporal information (the pinned part, so to speak) from intensional atoms.
  */
object PinnedAspToIncrementalAsp {
  def unpin(pinned: PinnedAtom) = pinned.atom

  def apply(rule: PinnedRule, atomsToUnpin: Set[ExtendedAtom]): AspRule[Atom] = {

    def unpinIfNeeded(pinned: PinnedAtom) = atomsToUnpin.contains(pinned) match {
      case true => unpin(pinned)
      case false => pinned
    }

    AspRule(
      unpin(rule.head),
      rule.pos filterNot (_.atom == now) map unpinIfNeeded,
      rule.neg map unpinIfNeeded
    )
  }

  def apply(p: MappedProgram): NormalProgram = {
    val headAtoms = p.mappedRules.flatMap(r => r._2 map (_.head)).toSet[ExtendedAtom] //i.e., intensional atoms

    val semiPinnedRules = p.rules map (r => apply(r, headAtoms))

    AspProgram(semiPinnedRules.toList)
  }
}
