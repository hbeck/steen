package jtms

import core._

import scala.annotation.tailrec
import scala.collection.mutable.{HashMap, Map, Set}

object JTMNRefactored {

  def apply(P: AspProgram): JTMNRefactored = {
    val tmn = new JTMNRefactored()
    P.rules foreach tmn.add
    tmn
  }

}

/**
  * justification-based truth maintenance network
  *
  * based book chapter from Beierle and Kern-Isberner
  *
  * Created by hb on 12/22/15.
  */
case class JTMNRefactored() {

  var rules: List[AspRule] = List()

  val cons: Map[Atom, Set[Atom]] = new HashMap[Atom, Set[Atom]]
  val supp: Map[Atom, Set[Atom]] = new HashMap[Atom, Set[Atom]]
  val suppRule: Map[Atom, Option[AspRule]] = new HashMap[Atom, Option[AspRule]]
  val status: Map[Atom, Status] = new HashMap[Atom, Status] //at least 'in' consequence of SuppRule

  def getModel(): Option[scala.collection.immutable.Set[Atom]] = {
    val atoms = inAtoms()
    if (atoms exists contradictionAtom) return None
    Some(atoms.toSet)
  }

  def justifications(h: Atom) = rules filter (_.head == h)

  def atoms() = cons.keySet

  def contradictionAtom(a: Atom) = a.isInstanceOf[ContradictionAtom]

  def inAtoms() = status.keys filter (status(_) == in)

  def unknownAtoms() = status.keys filter (status(_) == unknown)

  //ACons(a) = {x ∈ Cons(a) | a ∈ Supp(x)}
  def ACons(a: Atom): Set[Atom] = cons(a) filter (supp(_) contains a)
  def aff(a:Atom) = ACons(a:Atom)

  def repercussions(a: Atom) = trans(ACons, a)

  def antecedents(a: Atom): Set[Atom] = {
    if (status(a) == in) return supp(a)
    Set()
  }

  def foundations(a: Atom) = trans(antecedents, a)

  def ancestors(a: Atom) = trans(supp, a)

  def isAssumption(a: Atom) = (status(a) == in) && !suppRule(a).get.neg.isEmpty

  def unknownCons(a: Atom) = cons(a) filter (status(_) == unknown)

  def valid(rule: AspRule) =
    (rule.pos forall (status(_) == in)) && (rule.neg forall (status(_) == out))

  def invalid(rule: AspRule) =
    (rule.pos exists (status(_) == out)) || (rule.neg exists (status(_) == in))

  def unfounded(rule: AspRule) =
    (rule.pos forall (status(_) == in)) && (!(rule.neg exists (status(_) == in))) && (rule.neg exists (status(_) == unknown))

  //JTMS update algorithm
  def add(rule: AspRule): Unit = {
    register(rule)
    if (status(rule.head) == in) return
    if (invalid(rule)) { supp(rule.head) += findSpoiler(rule).get; return }
    val atoms = repercussions(rule.head) + rule.head
    updateBeliefs(atoms)
  }

  def register(rule: AspRule): Unit = {
    if (rules contains rule) return //list representation!
    rules = rules :+ rule
    rule.atoms foreach register
    rule.body foreach (cons(_) += rule.head)
  }

  def register(a: Atom): Unit = {
    if (!status.isDefinedAt(a)) status(a) = out
    if (!cons.isDefinedAt(a)) cons(a) = Set[Atom]()
    if (!supp.isDefinedAt(a)) supp(a) = Set[Atom]()
    if (!suppRule.isDefinedAt(a)) suppRule(a) = None
  }

  def updateBeliefs(atoms: Set[Atom]): Boolean = {
    atoms foreach setUnknown //Marking the nodes
    atoms foreach determineAndPropagateStatus // Evaluating the nodes' justifications
    atoms foreach fixAndPropagateStatus // Relaxing circularities (might lead to contradictions)
    tryEnsureConsistency
  }

  def setIn(rule: AspRule) = {
    status(rule.head) = in
    supp(rule.head) = Set() ++ rule.body
    suppRule(rule.head) = Some(rule)
  }

  def setOut(a: Atom) = {
    status(a) = out
    val maybeAtoms: List[Option[Atom]] = justifications(a) map (findSpoiler(_))
    supp(a) = Set() ++ (maybeAtoms filter (_.isDefined)) map (_.get)
    //Supp(a) = Set() ++ (justifications(a) map (findSpoiler(_).get))
    suppRule(a) = None
  }

  def setUnknown(atom: Atom) = {
    status(atom) = unknown
    supp(atom) = Set()
    suppRule(atom) = None
  }

  def findSpoiler(rule: AspRule): Option[Atom] = {
    if (math.random < 0.5) {
      rule.pos find (status(_) == out) match {
        case None => rule.neg find (status(_) == in)
        case opt => opt
      }
    } else {
      rule.neg find (status(_) == in) match {
        case None => rule.pos find (status(_) == out)
        case opt => opt
      }
    }
  }

  def determineAndPropagateStatus(a: Atom): Unit = {
    if (status(a) != unknown)
      return

    if (validation(a) || invalidation(a))
      unknownCons(a) foreach determineAndPropagateStatus
  }

  def validation(a: Atom): Boolean = {
    justifications(a) find valid match {
      case Some(rule) => setIn(rule); true
      case None => false
    }
  }

  def invalidation(a: Atom): Boolean = {
    if (justifications(a) forall invalid) {
      setOut(a)
      return true
    }
    false
  }

  def fixAndPropagateStatus(a: Atom): Unit = {
    if (status(a) != unknown)
      return

    if (fix(a)) {
      unknownCons(a) foreach fixAndPropagateStatus
    } else {
      val affected = ACons(a) + a
      affected foreach setUnknown
      affected foreach fixAndPropagateStatus
    }
  }

  def fix(a: Atom): Boolean = {
    justifications(a) find unfounded match {
      case Some(rule) => {
          if (ACons(a).isEmpty) fixIn(rule)
          else return false
      }
      case None => fixOut(a)
    }
    true
  }

  def fixIn(unfoundedValidRule: AspRule) = {
    unfoundedValidRule.neg filter (status(_) == unknown) foreach setOut //fix ancestors
    setIn(unfoundedValidRule)
  }

  def fixOut(a: Atom) = {
    //val unknownPosAtoms = justifications(a) map { r => (r.pos find (status(_)==unknown)).get }
    val maybeAtoms: List[Option[Atom]] = justifications(a) map { r => (r.pos find (status(_)==unknown)) }
    val unknownPosAtoms = (maybeAtoms filter (_.isDefined)) map (_.get)
    unknownPosAtoms foreach setOut //fix ancestors
    setOut(a)
  }

  def trans[T](f: T => Set[T], t: T): Set[T] = {
    trans(f)(f(t))
  }

  @tailrec
  final def trans[T](f: T => Set[T])(s: Set[T]): Set[T] = {
    val next = s.flatMap(f)
    val nextSet = next ++ s
    if (s == nextSet || next.isEmpty) {
      return s
    }
    trans(f)(nextSet)
  }

  //return false if called DDB method leaves without resolving a contradiction
  def tryEnsureConsistency(): Boolean = {
    for (c <- inAtoms() filter contradictionAtom) {
      if (!DDB(c)) return false
    }
    true
  }

  //return true if method leaves with status(c) != in
  def DDB(c: Atom): Boolean = {

    if (status(c) != in) return true

    val asms = foundations(c) filter isAssumption
    val maxAssumptions = asms filter { a =>
      ! ((asms - a) exists (b => foundations(b) contains a))
    }

    if (maxAssumptions.isEmpty)
      return false //contradiction cannot be solved

    findBacktrackingRule(maxAssumptions) match {
      case Some(rule) => add(rule); return true
      case None => return false
    }

  }

  def findBacktrackingRule(maxAssumptions: Set[Atom]): Option[AspRuleFromBacktracking] = {

    val culprit = maxAssumptions.head
    val n = suppRule(culprit).get.neg.head //(all .neg have status out at this point)

    val suppRules = maxAssumptions map (suppRule(_).get)
    val pos = suppRules flatMap (_.pos)
    val neg = (suppRules flatMap (_.neg)) - n
    val rule = AspRuleFromBacktracking(pos, neg, n)

    Some(rule)
  }


  /* ----------------------- in progress ... ------------------------------------- */

  // ----------------- test stuff or stuff that might not be needed ----------------

  /** @return true if M is admissible **/
  def set(M: collection.immutable.Set[Atom]): Boolean = { //TODO (HB) Set vs List. Always list for order?
  val m = M.toList
    for (i <- 0 to M.size - 1) {
      val rule: Option[AspRule] = findSuppRule(m, i)
      if (rule.isEmpty) {
        return false
      }
      setIn(rule.get)
    }
    for (n <- atoms diff M) {
      setOut(n)
    }
    true
  }

  /** takes atoms at list M index idx and tries to find a valid rule
    * that is founded wrt indexes 0..idx-1
    */
  def findSuppRule(M: List[Atom], idx: Int): Option[AspRule] = {
    val n = M(idx)
    val MSub = M.take(idx).toSet
    val rules = justifications(n).filter(rule => rule.pos.subsetOf(MSub) && rule.neg.intersect(M.toSet).isEmpty)
    selectRule(rules)
  }

  def selectRule(rules: List[AspRule]): Option[AspRule] = {
    if (rules.isEmpty)
      return None
    Some(rules.head)
  }

}