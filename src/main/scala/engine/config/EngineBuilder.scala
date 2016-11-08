package engine.config

import java.util.concurrent.TimeUnit

import clingo.ClingoConversion
import core.lars.{EngineTick, LarsProgram}
import engine.EvaluationEngine
import engine.asp._
import engine.asp.oneshot._
import engine.asp.tms.TmsEvaluationEngine
import engine.asp.tms.policies.{ImmediatelyAddRemovePolicy, TmsPolicy}
import jtms.algorithms.JtmsGreedy
import jtms.storage.OptimizedJtmsStorage
import jtms.{JtmsStorage, JtmsUpdateAlgorithm}

import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by FM on 14.05.16.
  */
object BuildEngine {
  def withProgram(program: LarsProgram) = EngineEvaluationConfiguration(program)
}

case class EngineEvaluationConfiguration(larsProgram: LarsProgram) {

  def withConfiguration(evaluationType: String, evaluationModifier: String) = ArgumentBasedConfiguration(larsProgram).build(evaluationType, evaluationModifier)

  def configure(tickSize: EngineTick = 1 second) = AspEngineEvaluationConfiguration(LarsToPinnedProgram(tickSize)(larsProgram))

}


case class AspEngineEvaluationConfiguration(pinnedProgram: PinnedProgramWithLars) {

  def withClingo() = EvaluationModeConfiguration(StreamingClingoInterpreter(ClingoConversion(pinnedProgram)))

  def withTms() = TmsConfiguration(pinnedProgram)

}

case class TmsConfiguration(pinnedProgram: PinnedProgramWithLars, policy: TmsPolicy = ImmediatelyAddRemovePolicy(JtmsGreedy(new OptimizedJtmsStorage(), new Random))) {

  def withRandom(random: Random) = TmsConfiguration(pinnedProgram, ImmediatelyAddRemovePolicy(JtmsGreedy(new OptimizedJtmsStorage(), random)))

  def useTms(jtms: JtmsUpdateAlgorithm) = TmsConfiguration(pinnedProgram, ImmediatelyAddRemovePolicy(jtms))

  def withPolicy(tmsPolicy: TmsPolicy) = TmsConfiguration(pinnedProgram, tmsPolicy)

}

object TmsConfiguration {
  implicit def toEvaluationModeConfig(config: TmsConfiguration): StartableEngineConfiguration = StartableEngineConfiguration(TmsEvaluationEngine(config.pinnedProgram, config.policy))
}

case class EvaluationModeConfiguration(streamingAspInterpreter: StreamingAspInterpreter) {

  def use(evaluationMode: EvaluationMode = Direct) = {
    val aspEvaluation = buildEvaluationMode(OneShotEvaluationEngine(streamingAspInterpreter), evaluationMode)
    EvaluationStrategyConfiguration(aspEvaluation)
  }

  private def buildEvaluationMode(aspEvaluation: OneShotEvaluation, evaluationMode: EvaluationMode) = evaluationMode match {
    case UseFuture(waitingAtMost: Duration) => FutureStreamingAspInterpreter(aspEvaluation, waitingAtMost)
    case _ => aspEvaluation
  }
}

case class EvaluationStrategyConfiguration(aspEvaluation: OneShotEvaluation) {

  def usePull() = StartableEngineConfiguration(AspPullEvaluationEngine(aspEvaluation))

  def usePush() = StartableEngineConfiguration(AspPushEvaluationEngine(aspEvaluation))

}

case class StartableEngineConfiguration(evaluationEngine: EvaluationEngine) {
  def start() = evaluationEngine
}
