package jtms.asp.examples

import clingo.ClingoCall
import core.Evaluation
import iclp.evaluation.{JtmsBeierleFixedEvaluation, JtmsDoyleEvaluation, JtmsDoyleHeuristicsEvaluation, JtmsGreedyEvaluation}
import org.scalatest.FlatSpec

/**
  * Created by FM on 25.02.16.
  */
trait EvaluateJtmsImplementations {
  this: FlatSpec =>

  val asp = ClingoCall()
  val jtmsBeierleFixed = new JtmsBeierleFixedEvaluation
  val jtmsDoyleHeuristics = new JtmsDoyleHeuristicsEvaluation
  val jtmsDoyle = new JtmsDoyleEvaluation
  val jtmsGreedy = new JtmsGreedyEvaluation

  def theSame(tests: => Evaluation => Unit) = {
    "The ASP implementation" should behave like tests(asp)
    "The JtmsBeierleFixed implementation" should behave like tests(jtmsBeierleFixed)
    "The JtmsDoyleHeuristics implementation" should behave like tests(jtmsDoyleHeuristics)
    "The JtmsDoyle implementation" should behave like tests(jtmsDoyle)
    "The JtmsGreedy implementation" should behave like tests(jtmsGreedy)
  }
}
