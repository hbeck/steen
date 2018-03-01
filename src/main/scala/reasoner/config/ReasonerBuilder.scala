package reasoner.config

import core.Atom
import core.lars.{ClockTime, LarsProgram}
import reasoner.asp._
import reasoner.asp.clingo.{ClingoConversion, ClingoProgramWithLars, StreamingClingoInterpreter}
import reasoner.common.{LarsProgramEncoding, PlainLarsToAspMapper}
import reasoner.config.EvaluationModifier.EvaluationModifier
import reasoner.config.ReasonerChoice.ReasonerChoice
import reasoner.incremental.jtms.algorithms.Jtms
import reasoner.incremental.{IncrementalReasoner, IncrementalRuleMaker}
import reasoner.{Reasoner, ReasonerWithFilter, ResultFilter}

import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by FM on 14.05.16.
  */
object BuildReasoner {
  def withProgram(program: LarsProgram) = Configuration(program)
}

case class Configuration(larsProgram: LarsProgram, clockTime: ClockTime = 1 second) {

  def withReasoning(reasonerChoice: ReasonerChoice, evaluationModifier: EvaluationModifier) = ArgumentBasedConfiguration(this).build(reasonerChoice, evaluationModifier)

  def configure() = ReasonerConfiguration(larsProgram, clockTime)

  def withClockTime(clockTime: ClockTime) = Configuration(larsProgram, clockTime)
}

case class ReasonerConfiguration(program: LarsProgram, clockTime: ClockTime) {

  private lazy val larsProgramEncoding = PlainLarsToAspMapper(clockTime)(program)

  def withClingo() = EvaluationModeConfiguration(ClingoConversion.fromLars(larsProgramEncoding))

  def withIncremental(): IncrementalConfiguration = IncrementalConfiguration(larsProgramEncoding)

}

case class IncrementalConfiguration(larsProgramEncoding: LarsProgramEncoding, jtms: Jtms = Jtms()) {

  def withRandom(random: Random) = IncrementalConfiguration(larsProgramEncoding, Jtms(jtms.network, random))

  def withJtms(jtms: Jtms) = IncrementalConfiguration(larsProgramEncoding, jtms)

  def use() = PreparedReasonerConfiguration(
    IncrementalReasoner(IncrementalRuleMaker(larsProgramEncoding), jtms),
    larsProgramEncoding.intensionalAtoms ++ larsProgramEncoding.signals
  )

}

object IncrementalConfiguration {
  implicit def toEvaluationModeConfig(config: IncrementalConfiguration): PreparedReasonerConfiguration =
    PreparedReasonerConfiguration(
      IncrementalReasoner(IncrementalRuleMaker(config.larsProgramEncoding), config.jtms),
      config.larsProgramEncoding.intensionalAtoms ++ config.larsProgramEncoding.signals
    )
}

case class EvaluationModeConfiguration(clingoProgram: ClingoProgramWithLars) {

  def withDefaultEvaluationMode() = withEvaluationMode(Direct)

  def withEvaluationMode(evaluationMode: EvaluationMode) = {
    val clingoEvaluation = buildEvaluationMode(OneShotClingoEvaluation(clingoProgram, StreamingClingoInterpreter(clingoProgram)), evaluationMode)
    EvaluationStrategyConfiguration(clingoEvaluation)
  }

  private def buildEvaluationMode(clingoEvaluation: ClingoEvaluation, evaluationMode: EvaluationMode) = evaluationMode match {
    case UseFuture(waitingAtMost: Duration) => FutureStreamingAspInterpreter(clingoEvaluation, waitingAtMost)
    case _ => clingoEvaluation
  }
}

case class EvaluationStrategyConfiguration(clingoEvaluation: ClingoEvaluation) {

  def usePull() = PreparedReasonerConfiguration(
    AspPullReasoner(clingoEvaluation),
    clingoEvaluation.program.intensionalAtoms ++ clingoEvaluation.program.signals
  )

  def usePush() = PreparedReasonerConfiguration(
    AspPushReasoner(clingoEvaluation),
    clingoEvaluation.program.intensionalAtoms ++ clingoEvaluation.program.signals
  )

}

case class PreparedReasonerConfiguration(reasoner: Reasoner, restrictTo: Set[Atom]) {

  def withFilter(restrictTo: Set[Atom]) = PreparedReasonerConfiguration(reasoner, restrictTo)

  def seal() = ReasonerWithFilter(reasoner, ResultFilter(restrictTo))

}