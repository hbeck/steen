package core

import core.lars.{Assignment, ExtendedAtom, HeadAtom}

/**
  * Created by FM on 15.06.16.
  */
trait Rule[THead <: HeadAtom, TBody <: ExtendedAtom] {
  val head: THead
  val pos: Set[TBody]
  val neg: Set[TBody]

  lazy val body = pos union neg

  lazy val isFact: Boolean = pos.isEmpty && neg.isEmpty

  //lazy val atoms: = body ++ Set(head) //TODO type
  def atoms: Set[TBody]

  lazy val isGround: Boolean = atoms forall (_.isGround)

  def assign(assignment: Assignment): Rule[THead,TBody] = {
    val assignedHead: THead = head.assign(assignment).asInstanceOf[THead]
    val assignedPosBody = pos map (x => x.assign(assignment).asInstanceOf[TBody])
    val assignedNegBody = neg map (x => x.assign(assignment).asInstanceOf[TBody])
    from(assignedHead,assignedPosBody,assignedNegBody)
  }

  def variables(): Set[Variable] = {
    if (isGround) return Set()
    atoms flatMap {
      case a: AtomWithArgument => a.arguments filter (_.isInstanceOf[Variable]) map (_.asInstanceOf[Variable])
      case _ => Set()
    }
  }

  //naming it 'apply' causes problems in case classes (ambiguity with use of constructor)
  def from(head: THead, pos: Set[TBody], neg: Set[TBody]): Rule[THead, TBody]

  def ==(other: Rule[THead, TBody]): Boolean = {
    if (this.head != other.head) return false
    if (this.pos != other.pos) return false
    if (this.neg != other.neg) return false
    true
  }

  override def equals(other: Any): Boolean = other match {
    case r: Rule[THead, TBody] => this == r
    case _ => {
      println("this:  "+this.getClass)
      println("other: "+other.getClass)
      false
    }
  }
}

trait Fact[THead <: HeadAtom, TBody <: ExtendedAtom] extends Rule[THead, TBody] {
  val pos: Set[TBody] = Set()
  val neg: Set[TBody] = Set()
  //override def isGround(): Boolean = head.isGround()
  override lazy val isFact: Boolean = true
}