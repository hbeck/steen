package engine.implementations

import asp.{Asp, AspConversion}
import core.{AtomWithArguments, Fact, Program}
import engine._

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


/**
  * Created by FM on 21.04.16.
  */

case class AspPullEvaluation(private val aspEvaluation: AspEvaluation) extends EvaluationEngine {

  val atomStream: OrderedAtomStream = new OrderedAtomStream

  val cachedResults = scala.collection.mutable.HashMap[Time, Result]()

  def prepare(time: Time) = {
    val result = aspEvaluation.prepare(time, atomStream.evaluate(time))

    cachedResults.put(time, result)
  }

  def evaluate(time: Time) = {
    if (!cachedResults.contains(time))
      prepare(time)
    cachedResults(time)
  }

  override def append(time: Time)(atoms: Atom*): Unit = {
    atomStream.append(time)(atoms.toSet)
    // TODO: implement invalidation of result
    // the remove is probably not enough (==> invalidate previous fetched results)
    cachedResults.remove(time)
  }
}
