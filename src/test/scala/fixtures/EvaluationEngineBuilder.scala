package fixtures

import core.lars.Program
import engine.EvaluationEngine
import engine.config.BuildEngine

/**
  * Created by FM on 01.06.16.
  */


trait EvaluationEngineBuilder {
  type EngineBuilder = ((Program) => EvaluationEngine)
  val defaultEngine: EngineBuilder
}

trait ClingoPullEngine extends EvaluationEngineBuilder {
  val defaultEngine = (p: Program) => BuildEngine.withProgram(p).useAsp().withClingo().use().usePull().start()
}

trait ClingoPushEngine extends EvaluationEngineBuilder {
  val defaultEngine = (p: Program) => BuildEngine.withProgram(p).useAsp().withClingo().use().usePush().start()
}

trait TmsPushEngine extends EvaluationEngineBuilder {
  val defaultEngine = (p: Program) => BuildEngine.withProgram(p).useAsp().withTms().use().usePush().start()
}