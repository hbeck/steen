package jtms.tmn

import core.asp.{AspFact, NormalRule}
import core.{GroundAtom, Atom, Predicate}
import jtms._
import org.scalatest.FlatSpec

/**
  * Created by FM on 05.02.16.
  */
class JtmnSpec extends FlatSpec {
  def Assumption(node: String): NormalRule = Assumption(GroundAtom(Predicate(node)))

  def Assumption(node: Atom): NormalRule = AspFact(node)

  def EmptyTmn = new JtmsDoyle()
}
