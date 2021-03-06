package core.lars

import core._
import core.asp._
import core.grounding.{Grounding, LarsGrounding}
import evaluation.iclp.Util._
import reasoner.incremental.jtms.algorithms.Jtms
import org.scalatest.FunSuite
import engine.Load
import engine.Load._

/**
  * Created by hb on 8/23/16.
  */
class GrounderTests extends FunSuite {

  def jtmsInst(P: NormalProgram): Jtms = Jtms(P)

  def ground(p: LarsProgram) = LarsGrounding(p).groundProgram
  def program(rules: LarsRule*): LarsProgram = LarsProgram(rules)

  val load = Load()
  import load._

  test("parsing") {
    assert(xatom("a")==PredicateAtom(p("a")))
    assert(xatom("a(x)")==GroundAtomWithArguments(Predicate("a"),Seq[Value](StringValue("x"))))
    assert(xatom("a(x,y)")==GroundAtomWithArguments(Predicate("a"),Seq[Value](StringValue("x"),StringValue("y"))))
    assert(xatom("a(X)")==NonGroundAtom(Predicate("a"),Seq[Argument]("X")))
    assert(xatom("a(X,y)")==NonGroundAtom(Predicate("a"),Seq[Argument]("X",StringValue("y"))))
    assert(xatom("a(y,X)")==NonGroundAtom(Predicate("a"),Seq[Argument](StringValue("y"),"X")))
  }

  //
  //
  //

  test("gt1") {
    val r1:LarsRule = fact("a")
    val rules:Seq[LarsRule] = Seq(r1)
    val p = LarsProgram(rules)
    assert(ground(p) == p)
  }

  test("gt2") {
    val r1 = fact("a(x)")
    val rules:Seq[LarsRule] = Seq(r1)
    val p = LarsProgram(rules)
    assert(ground(p) ==p)
  }

  test("gt3") {

    val r1 = fact("a(x)")
    val r2 = rule("b(V) :- a(V)")
    val p = program(r1,r2)

    val gr1 = r1
    val gr2 = rule("b(x) :- a(x)")
    val gp = program(gr1,gr2)
    val grounder = LarsGrounding(p)

    //grounder.inspect.rules foreach println

    assert(grounder.groundProgram==gp)

    val model = modelFromClingo("a(x) b(x)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)

  }

  test("gt4") {

    val r1 = fact("a(x)")
    val r2 = rule("b(V) :- c(V)")
    val r3 = rule("c(V) :- a(V)")
    val p = program(r1,r2,r3)

    val gr1 = r1
    val gr2 = rule("b(x) :- c(x)")
    val gr3 = rule("c(x) :- a(x)")
    val gp = program(gr1,gr2,gr3)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r2,v("V")) == Set(strVal("x")))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("V")) == Set(strVal("x")))

    assert(grounder.groundProgram==gp)

    val model = modelFromClingo("a(x) c(x) b(x)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  val X = v("X")
  val Y = v("Y")
  val Z = v("Z")
  val x1 = strVal("x1")
  val x2 = strVal("x2")
  val y1 = strVal("y1")
  val y2 = strVal("y2")
  val z1 = strVal("z1")
  val z2 = strVal("z2")

  test("cross 1") {
    val xSets = Set(Set((X,x1)),Set((X,x2)))
    val ySets = Set(Set((Y,y1)),Set((Y,y2)))

    val expectedCross = Set(Set((X,x1),(Y,y1)),Set((X,x1),(Y,y2)),Set((X,x2),(Y,y1)),Set((X,x2),(Y,y2)))

    assert(Grounding.cross(xSets,ySets) == expectedCross)
  }

  test("cross 2") {
    val cross1:Set[Set[(Variable,Value)]] = Set(Set((X,x1),(Y,y1)),Set((X,x1),(Y,y2)),Set((X,x2),(Y,y1)),Set((X,x2),(Y,y2)))
    val zSets = Set(Set((Z,z1)),Set((Z,z2)))

    val expected: Set[Set[(Variable,Value)]] =
      Set(Set((X,x1),(Y,y1),(Z,z1)),Set((X,x1),(Y,y2),(Z,z1)),Set((X,x2),(Y,y1),(Z,z1)),Set((X,x2),(Y,y2),(Z,z1)),
        Set((X,x1),(Y,y1),(Z,z2)),Set((X,x1),(Y,y2),(Z,z2)),Set((X,x2),(Y,y1),(Z,z2)),Set((X,x2),(Y,y2),(Z,z2)))

    assert(Grounding.cross(cross1,zSets) == expected)
  }

  test("cross reduce ") {
    val xSet = Set(Set((X,x1)),Set((X,x2)))
    val ySet = Set(Set((Y,y1)),Set((Y,y2)))
    val zSet = Set(Set((Z,z1)),Set((Z,z2)))

    val expected: Set[Set[(Variable,Value)]] =
      Set(Set((X,x1),(Y,y1),(Z,z1)),Set((X,x1),(Y,y2),(Z,z1)),Set((X,x2),(Y,y1),(Z,z1)),Set((X,x2),(Y,y2),(Z,z1)),
          Set((X,x1),(Y,y1),(Z,z2)),Set((X,x1),(Y,y2),(Z,z2)),Set((X,x2),(Y,y1),(Z,z2)),Set((X,x2),(Y,y2),(Z,z2)))

    val result: Set[Set[(Variable, Value)]] = Seq(xSet,ySet,zSet).reduce((s1, s2) => Grounding.cross(s1,s2))

    assert(result == expected)
  }

  /*
  test("create assignment 1") {

    val possibleValuesPerVariable: Map[Variable,Set[Value]] = Map(X -> Set(x1,x2), Y -> Set(y1,y2), Z -> Set(z1,z2))

    val a1 = Assignment(Map[Variable,Value](X -> x1, Y -> y1, Z -> z1))
    val a2 = Assignment(Map[Variable,Value](X -> x1, Y -> y1, Z -> z2))
    val a3 = Assignment(Map[Variable,Value](X -> x1, Y -> y2, Z -> z1))
    val a4 = Assignment(Map[Variable,Value](X -> x1, Y -> y2, Z -> z2))
    val a5 = Assignment(Map[Variable,Value](X -> x2, Y -> y1, Z -> z1))
    val a6 = Assignment(Map[Variable,Value](X -> x2, Y -> y1, Z -> z2))
    val a7 = Assignment(Map[Variable,Value](X -> x2, Y -> y2, Z -> z1))
    val a8 = Assignment(Map[Variable,Value](X -> x2, Y -> y2, Z -> z2))

    val expectedAssignments: Set[Assignment] = Set(a1,a2,a3,a4,a4,a5,a6,a7,a8)

    assert(Grounder.createAssignments(possibleValuesPerVariable) == expectedAssignments)
  }
  */

  test("gt5") {

    val ax = fact("a(x)")
    val ay = fact("a(y)")
    val r3 = rule("b(V) :- a(V)")
    val p = program(ax,ay,r3)

    val gax = ax
    val gay = ay
    val gr3x = rule("b(x) :- a(x)")
    val gr3y = rule("b(y) :- a(y)")
    val gp = program(gax,gay,gr3x,gr3y)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r3,v("V")) == strVals("x","y"))
    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x) a(y) b(x) b(y)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt6") {

    val ax = fact("a(x)")
    val ay = fact("a(y)")
    val r2 = rule("b(V) :- c(V)")
    val r3 = rule("c(V) :- a(V)")
    val p = program(ax,ay,r2,r3)

    val gax = ax
    val gay = ay
    val gr2x = rule("b(x) :- c(x)")
    val gr2y = rule("b(y) :- c(y)")
    val gr3x = rule("c(x) :- a(x)")
    val gr3y = rule("c(y) :- a(y)")
    val gp = program(gax,gay,gr2x,gr2y,gr3x,gr3y)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r2,v("V")) == strVals("x","y"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("V")) == strVals("x","y"))

    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x) a(y) c(x) c(y) b(x) b(y)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt7") {

    val ax = fact("a(x)")
    val ay = fact("a(y)")
    val az = fact("a(z)")
    val r3 = rule("c(V) :- a(V)")
    val p = program(ax,ay,az,r3)

    val gax = ax
    val gay = ay
    val gaz = az
    val gr3x = rule("c(x) :- a(x)")
    val gr3y = rule("c(y) :- a(y)")
    val gr3z = rule("c(z) :- a(z)")
    val gp = program(gax,gay,gaz,gr3x,gr3y,gr3z)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r3,v("V")) == strVals("x","y","z"))

    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x) a(y) a(z) c(x) c(y) c(z)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt8") {

    val ax = fact("a(x)")
    val ay = fact("a(y)")
    val az = fact("a(z)")
    val r2 = rule("b(V) :- c(V)")
    val r3 = rule("c(V) :- a(V)")
    val p = program(ax,ay,az,r2,r3)

    val gax = ax
    val gay = ay
    val gaz = az
    val gr2x = rule("b(x) :- c(x)")
    val gr2y = rule("b(y) :- c(y)")
    val gr2z = rule("b(z) :- c(z)")
    val gr3x = rule("c(x) :- a(x)")
    val gr3y = rule("c(y) :- a(y)")
    val gr3z = rule("c(z) :- a(z)")
    val gp = program(gax,gay,gaz,gr2x,gr2y,gr2z,gr3x,gr3y,gr3z)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r2,v("V")) == strVals("x","y","z"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("V")) == strVals("x","y","z"))

//    println("expected program")
//    gp.rules foreach println
//
//    println("\ngrounded program")
//    grounder.groundProgram.rules foreach println

//    val onlyInComputed = for (r <- grounder.groundProgram.rules if (!gp.rules.contains(r))) yield r
//    val onlyInExpected = for (r <- gp.rules if (!grounder.groundProgram.rules.contains(r))) yield r
//
//    println("only in computed: "+onlyInComputed)
//    println("only in expected: "+onlyInExpected)

    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x) a(y) a(z) c(x) c(y) c(z) b(x) b(y) b(z)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("grounding rules") {
    val ri1 = rule("i(X,Y) :- a(X), b(Y)")
    assert(!ri1.isGround)
    assert(ri1.atoms forall (_.isInstanceOf[NonGroundAtom]))
    val a1 = Assignment(Map(v("X") -> "x1", v("Y") -> "y1"))
    val gri1 = ri1.assign(a1)
    assert(gri1.isGround)
    assert(gri1 == rule("i(x1,y1) :- a(x1), b(y1)"))

    val ri2 = rule("i(X,Y) :- a(X), b(Y), not c(Y), not d(Y)")
    assert(!ri2.isGround)
    assert(ri2.atoms forall (_.isInstanceOf[NonGroundAtom]))
    val a2 = Assignment(Map(v("X") -> "x1", v("Y") -> "y1"))
    val gri2 = ri2.assign(a2)
    assert(gri2.isGround)
    assert(gri2 == rule("i(x1,y1) :- a(x1), b(y1), not c(y1), not d(y1)"))
  }

  test("gt9") {

    val ax1 = fact("a(x1)")
    val ax2 = fact("a(x2)")
    val bx1 = fact("b(y1)")
    val bx2 = fact("b(y2)")

    val ri = rule("i(X,Y) :- a(X), b(Y)")
    val rj = rule("j(X) :- i(X,Y)")
    val p = program(ax1,ax2,bx1,bx2,ri,rj)

    val manualGrounding: Set[LarsRule] = {
      for (x <- Set("x1", "x2"); y <- Set("y1", "y2")) yield {
        val a = Assignment(Map(v("X") -> x, v("Y") -> y))
        val gri: LarsRule = ri.assign(a)
        val grj: LarsRule = rj.assign(a)
        Set[LarsRule](gri, grj)
      }
    }.flatten

    val rules = Seq[LarsRule](ax1,ax2,bx1,bx2) ++ manualGrounding

    val gp = LarsProgram(rules)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(ri,v("X")) == strVals("x1","x2"))
    assert(grounder.inspect.possibleValuesForVariable(ri,v("Y")) == strVals("y1","y2"))
    assert(grounder.inspect.possibleValuesForVariable(rj,v("X")) == strVals("x1","x2"))
    assert(grounder.inspect.possibleValuesForVariable(rj,v("Y")) == strVals("y1","y2"))
    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x1) a(x2) b(y1) b(y2) i(x1,y1) i(x2,y1) i(x1,y2) i(x2,y2) j(x1) j(x2)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt10-pre") {

    val ax1 = fact("a(x1)")
    val ax2 = fact("a(x2)")
    val by = fact("b(y)")

    val ri1 = rule("i(X,Y) :- a(X), b(Y)")
    val ri2 = rule("i(X,Y) :- i(Y,X)")
    //val ri2 = rule("i(Y,X) :- i(X,Y)")

    val p = program(ax1,ax2,by,ri1,ri2)

    val manualGrounding: Set[LarsRule] = Set(
      "i(x1,y) :- a(x1), b(y)",
      "i(x2,y) :- a(x2), b(y)",
      "i(x1,x1) :- i(x1,x1)",
      "i(x2,x2) :- i(x2,x2)",
      "i(x1,x2) :- i(x2,x1)",
      "i(x2,x1) :- i(x1,x2)",
      "i(x1,y) :- i(y,x1)",
      "i(x2,y) :- i(y,x2)",
      "i(y,y) :- i(y,y)",
      "i(y,x1) :- i(x1,y)",
      "i(y,x2) :- i(x2,y)"
    ) map (rule(_))

    val rules = Seq[LarsRule](ax1,ax2,by) ++ manualGrounding

    val gp = LarsProgram(rules)
    val larsGrounding = LarsGrounding(p)

    def pos(rule: LarsRule, variable: String, strings: Set[String]) = {
      assert(larsGrounding.inspect.possibleValuesForVariable(rule,v(variable)) == strings.map(Value(_)))
    }

    pos(ri1,"X",Set("x1","x2"))
    pos(ri1,"Y",Set("y"))
    pos(ri2,"X",Set("x1","x2","y"))
    pos(ri2,"Y",Set("x1","x2","y"))

    assert(larsGrounding.groundProgram.rules.contains(rule("i(y,x1) :- i(x1,y)")))
    assert(larsGrounding.groundProgram.rules.contains(rule("i(x1,y) :- i(y,x1)")))

    //println(LarsProgram(larsGrounding.groundProgram.rules))

    //    val onlyInComputed = for (r <- larsGrounding.groundProgram.rules if (!gp.rules.contains(r))) yield r
    //    val onlyInExpected = for (r <- gp.rules if (!larsGrounding.groundProgram.rules.contains(r))) yield r
    //
    //    println("only in computed: "+LarsProgram(onlyInComputed))
    //    println("only in expected: "+LarsProgram(onlyInExpected))

    assert(larsGrounding.groundProgram == gp)

    val model = modelFromClingo("b(y) a(x1) a(x2) i(x1,y) i(x2,y) i(y,x2) i(y,x1)")

    val asp = asAspProgram(larsGrounding.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }


  test("gt10") {

    val ax1 = fact("a(x1)")
    val ax2 = fact("a(x2)")
    val by3 = fact("b(y3)")
    val by4 = fact("b(y4)")

    val ri1 = rule("i(X,Y) :- a(X), b(Y)")
    val ri2 = rule("i(X,Y) :- i(Y,X)")
    val rj1 = rule("j(X,Y) :- a(X), b(Y)")
    val rj2 = rule("j(Y,X) :- j(X,Y)")
    val rk1 = rule("k(X,Y) :- i(X,Y)")
    val rk2 = rule("k(X,Y) :- i(Y,X)")
    val rk3 = rule("k(Y,X) :- i(X,Y)")
    val rk4 = rule("k(Y,X) :- i(Y,X)")

    val p = program(ax1,ax2,by3,by4,ri1,ri2,rj1,rj2,rk1,rk2,rk3,rk4)

    val manualGrounding: Set[LarsRule] = {
      for (x <- Set("x1", "x2"); y <- Set("y3", "y4")) yield {
        val axy = Assignment(Map(v("X") -> x, v("Y") -> y))
        val ayx = Assignment(Map(v("Y") -> x, v("X") -> y))
        Set[LarsRule]() ++
          (Set(ri1,rj1) map (_.assign(axy))) ++
          (Set(ri2,rj2,rk1,rk2,rk3,rk4) flatMap (r => Set(r.assign(axy),r.assign(ayx))))
      }
    }.flatten

    val rules = Seq[LarsRule](ax1,ax2,by3,by4) ++ manualGrounding

    val gp = LarsProgram(rules)
    val larsGrounding = LarsGrounding(p)

    def pos(rule: LarsRule, variable: String, strings: Set[String]) = {
      assert(larsGrounding.inspect.possibleValuesForVariable(rule,v(variable)) == strings.map(Value(_)))
    }

    pos(ri1,"X",Set("x1","x2"))
    pos(ri1,"Y",Set("y3","y4"))
    pos(ri2,"X",Set("x1","x2","y3","y4"))
    pos(ri2,"Y",Set("x1","x2","y3","y4"))

    pos(rj1,"X",Set("x1","x2"))
    pos(rj1,"Y",Set("y3","y4"))
    pos(rj2,"X",Set("x1","x2","y3","y4"))
    pos(rj2,"Y",Set("x1","x2","y3","y4"))

    for (rule <- Set[LarsRule](rk1,rk2,rk3,rk4)) {
      for (variable <- Set("X","Y")) {
        pos(rule,variable,Set("x1","x2","y3","y4"))
      }
    }

    for (ruleString <- Seq(
      "i(x1,y4) :- i(y4,x1)", "i(y4,x1) :- i(x1,y4)",
      "j(x1,y4) :- j(y4,x1)", "j(y4,x1) :- j(x1,y4)",
      "k(x1,y4) :- i(y4,x1)", "k(y4,x1) :- i(x1,y4)",
      "k(x1,y4) :- i(x1,y4)", "k(y4,x1) :- i(y4,x1)")) {

      assert(larsGrounding.groundProgram.rules.contains(rule(ruleString)))

    }

    //println(LarsProgram(larsGrounding.groundProgram.rules))

//    val onlyInComputed = for (r <- larsGrounding.groundProgram.rules if (!gp.rules.contains(r))) yield r
//    val onlyInExpected = for (r <- gp.rules if (!larsGrounding.groundProgram.rules.contains(r))) yield r
//
//    println("only in computed: "+LarsProgram(onlyInComputed))
//    println("only in expected: "+LarsProgram(onlyInExpected))

    val model = modelFromClingo("b(y3) b(y4) a(x1) a(x2) i(x1,y3) i(x2,y3) i(x1,y4) i(x2,y4) i(y4,x2) i(y4,x1) i(y3,x2) i(y3,x1) j(x1,y3) j(x2,y3) j(x1,y4) j(x2,y4) j(y4,x2) j(y4,x1) j(y3,x2) j(y3,x1) k(x1,y3) k(x2,y3) k(x1,y4) k(x2,y4) k(y4,x2) k(y4,x1) k(y3,x2) k(y3,x1)")

    val asp = asAspProgram(larsGrounding.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt11") {

    //transitivity

    val a12 = fact("a(x1,x2)")
    val a23 = fact("a(x2,x3)")
    val a34 = fact("a(x3,x4)")

    val ri1 = rule("i(X,Y) :- a(X,Y)")
    val ri2 = rule("i(X,Y) :- i(X,Z), i(Z,Y)")

    val p = program(a12,a23,a34,ri1,ri2)

    val manualGrounding: Set[LarsRule] = Set(
      rule("i(x3,x4) :- a(x3,x4)"),
      rule("i(x2,x2) :- a(x2,x2)"),
      rule("i(x1,x3) :- a(x1,x3)"),
      rule("i(x1,x4) :- a(x1,x4)"),
      rule("i(x3,x2) :- a(x3,x2)"),
      rule("i(x2,x3) :- a(x2,x3)"),
      rule("i(x1,x2) :- a(x1,x2)"),
      rule("i(x2,x4) :- a(x2,x4)"),
      rule("i(x3,x3) :- a(x3,x3)"),
      rule("i(x3,x2) :- i(x3,x3), i(x3,x2)"),
      rule("i(x3,x3) :- i(x3,x1), i(x1,x3)"),
      rule("i(x3,x4) :- i(x3,x1), i(x1,x4)"),
      rule("i(x3,x3) :- i(x3,x2), i(x2,x3)"),
      rule("i(x1,x3) :- i(x1,x1), i(x1,x3)"),
      rule("i(x1,x3) :- i(x1,x4), i(x4,x3)"),
      rule("i(x3,x3) :- i(x3,x4), i(x4,x3)"),
      rule("i(x2,x2) :- i(x2,x1), i(x1,x2)"),
      rule("i(x3,x2) :- i(x3,x2), i(x2,x2)"),
      rule("i(x1,x4) :- i(x1,x3), i(x3,x4)"),
      rule("i(x1,x4) :- i(x1,x2), i(x2,x4)"),
      rule("i(x1,x4) :- i(x1,x4), i(x4,x4)"),
      rule("i(x3,x3) :- i(x3,x3)"),
      rule("i(x3,x4) :- i(x3,x2), i(x2,x4)"),
      rule("i(x2,x3) :- i(x2,x2), i(x2,x3)"),
      rule("i(x1,x4) :- i(x1,x1), i(x1,x4)"),
      rule("i(x1,x2) :- i(x1,x1), i(x1,x2)"),
      rule("i(x1,x3) :- i(x1,x2), i(x2,x3)"),
      rule("i(x2,x3) :- i(x2,x3), i(x3,x3)"),
      rule("i(x1,x2) :- i(x1,x2), i(x2,x2)"),
      rule("i(x2,x4) :- i(x2,x2), i(x2,x4)"),
      rule("i(x3,x2) :- i(x3,x1), i(x1,x2)"),
      rule("i(x3,x2) :- i(x3,x4), i(x4,x2)"),
      rule("i(x2,x4) :- i(x2,x1), i(x1,x4)"),
      rule("i(x1,x2) :- i(x1,x3), i(x3,x2)"),
      rule("i(x3,x4) :- i(x3,x4), i(x4,x4)"),
      rule("i(x2,x3) :- i(x2,x1), i(x1,x3)"),
      rule("i(x2,x3) :- i(x2,x4), i(x4,x3)"),
      rule("i(x3,x4) :- i(x3,x3), i(x3,x4)"),
      rule("i(x2,x2) :- i(x2,x2)"),
      rule("i(x2,x2) :- i(x2,x3), i(x3,x2)"),
      rule("i(x1,x2) :- i(x1,x4), i(x4,x2)"),
      rule("i(x2,x2) :- i(x2,x4), i(x4,x2)"),
      rule("i(x2,x4) :- i(x2,x4), i(x4,x4)"),
      rule("i(x1,x3) :- i(x1,x3), i(x3,x3)"),
      rule("i(x2,x4) :- i(x2,x3), i(x3,x4)")
    )

    val rules = Seq[LarsRule](a12,a23,a34) ++ manualGrounding
    val gp = LarsProgram(rules)

    val grounder = LarsGrounding(p)

    //printInspect(grounder)

    assert(grounder.inspect.possibleValuesForVariable(ri1,v("X")) == strVals("x1","x2","x3"))
    assert(grounder.inspect.possibleValuesForVariable(ri1,v("Y")) == strVals("x2","x3","x4"))
    assert(grounder.inspect.possibleValuesForVariable(ri2,v("X")) == strVals("x1","x2","x3"))
    assert(grounder.inspect.possibleValuesForVariable(ri2,v("Y")) == strVals("x2","x3","x4"))
    assert(grounder.inspect.possibleValuesForVariable(ri2,v("Z")) == strVals("x1","x2","x3","x4"))

    //println(grounder.groundProgram.rules)

//    val onlyInComputed = for (r <- grounder.groundProgram.rules if (!gp.rules.contains(r))) yield r
//    val onlyInExpected = for (r <- gp.rules if (!grounder.groundProgram.rules.contains(r))) yield r
//
//    println("only in computed: "+LarsProgram(onlyInComputed))
//    println("only in expected: "+LarsProgram(onlyInExpected))

    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x1,x2) a(x2,x3) a(x3,x4) i(x1,x2) i(x2,x3) i(x3,x4) i(x1,x3) i(x2,x4) i(x1,x4)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)

  }



  test("gt12") {

    //symmetry

    val a1 = fact("a(x)")
    val b11 = fact("b(x,y1)")
    val b12 = fact("b(x,y2)")
    val c1 = fact("c(y1)")
    val c2 = fact("c(y2)")
    val d12 = fact("d(x,y2)")

    val r1 = rule("i(X,Y) :- a(X), i(X,Y)")
    val r2 = rule("i(X,Y) :- j(X,Y), not d(X,Y)")
    val r3 = rule("j(X,Y) :- b(X,Z), c(Y)")

    val p = program(a1,b11,b12,c1,c2,d12,r1,r2,r3)

    val manualGrounding: Set[LarsRule] = Set(
      rule("i(x,y1) :- a(x), i(x,y1)"),
      rule("i(x,y2) :- a(x), i(x,y2)"),
      rule("i(x,y1) :- j(x,y1), not d(x,y1)"),
      rule("i(x,y2) :- j(x,y2), not d(x,y2)"),
      rule("j(x,y1) :- b(x,y1), c(y1)"),
      rule("j(x,y2) :- b(x,y1), c(y2)"),
      rule("j(x,y1) :- b(x,y2), c(y1)"),
      rule("j(x,y2) :- b(x,y2), c(y2)")
    )

    val rules = Seq[LarsRule](a1,b11,b12,c1,c2,d12) ++ manualGrounding

    val gp = LarsProgram(rules)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r1,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r1,v("Y")) == strVals("y1","y2"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("Y")) == strVals("y1","y2"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("Y")) == strVals("y1","y2"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("Z")) == strVals("y1","y2"))

    //    println(LarsProgram(grounder.groundProgram.rules))
    //
    //    val onlyInComputed = for (r <- grounder.groundProgram.rules if (!gp.rules.contains(r))) yield r
    //    val onlyInExpected = for (r <- gp.rules if (!grounder.groundProgram.rules.contains(r))) yield r
    //
    //    println("only in computed: "+LarsProgram(onlyInComputed))
    //    println("only in expected: "+LarsProgram(onlyInExpected))
    //
    //    printInspect(grounder)

    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x) b(x,y1) b(x,y2) c(y1) c(y2) d(x,y2) j(x,y1) j(x,y2) i(x,y1)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt12w") {

    val a1 = fact("a(x)")
    val b11 = fact("b(x,y1)")
    val b12 = fact("b(x,y2)")
    val c1 = fact("c(y1)")
    val c2 = fact("c(y2)")
    val d12 = fact("d(x,y2)")

    val r1 = rule("i(X,Y) :- a(X), i(X,Y), w_7_d_sig1(Y)")
    val r2 = rule("i(X,Y) :- j(X,Y), not d(X,Y), not w_7_d_sig2(X,Y)")
    val r3 = rule("j(X,Y) :- b(X,Z), c(Y), not w_7_d_sig3(X,Z,Y)")

    val p = program(a1,b11,b12,c1,c2,d12,r1,r2,r3)

    val manualGrounding: Set[LarsRule] = Set(
      rule("i(x,y1) :- a(x), i(x,y1), w_7_d_sig1(y1)"),
      rule("i(x,y2) :- a(x), i(x,y2), w_7_d_sig1(y2)"),
      rule("i(x,y1) :- j(x,y1), not d(x,y1), not w_7_d_sig2(x,y1)"),
      rule("i(x,y2) :- j(x,y2), not d(x,y2), not w_7_d_sig2(x,y2)"),
      rule("j(x,y1) :- b(x,y1), c(y1), not w_7_d_sig3(x,y1,y1)"),
      rule("j(x,y2) :- b(x,y1), c(y2), not w_7_d_sig3(x,y1,y2)"),
      rule("j(x,y1) :- b(x,y2), c(y1), not w_7_d_sig3(x,y2,y1)"),
      rule("j(x,y2) :- b(x,y2), c(y2), not w_7_d_sig3(x,y2,y2)")
    )

    val rules = Seq[LarsRule](a1,b11,b12,c1,c2,d12) ++ manualGrounding

    val gp = LarsProgram(rules)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r1,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r1,v("Y")) == strVals("y1","y2"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("Y")) == strVals("y1","y2"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("Y")) == strVals("y1","y2"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("Z")) == strVals("y1","y2"))

    //    println(LarsProgram(grounder.groundProgram.rules))
    //
//        val onlyInComputed = for (r <- grounder.groundProgram.rules if (!gp.rules.contains(r))) yield r
//        val onlyInExpected = for (r <- gp.rules if (!grounder.groundProgram.rules.contains(r))) yield r
//
//        println("only in computed: "+LarsProgram(onlyInComputed))
//        println("only in expected: "+LarsProgram(onlyInExpected))
//
//        printInspect(grounder)

    assert(grounder.groundProgram == gp)

    //no semantics comparison with asp
  }

  test("gt13") {

    val a = fact("a(x,y)")
    val b = fact("b(y)")

    val r1 = rule("i(X,Y) :- a(X,Y), b(Y)")
    val r2 = rule("i(z,z) :- i(X,Y), not d(X,Y)")
    val r3 = rule("j(X,Y) :- i(X,Y)")

    val p = program(a,b,r1,r2,r3)

    val manualGrounding: Set[LarsRule] = Set(
      rule("i(x,y) :- a(x,y), b(y)"),
      rule("i(z,z) :- i(x,y), not d(x,y)"),
      rule("i(z,z) :- i(z,z), not d(z,z)"),
      rule("i(z,z) :- i(z,y), not d(z,y)"),
      rule("i(z,z) :- i(x,z), not d(x,z)"),
      rule("j(z,y) :- i(z,y)"),
      rule("j(x,z) :- i(x,z)"),
      rule("j(x,y) :- i(x,y)"),
      rule("j(z,z) :- i(z,z)")
    )

    val rules = Seq[LarsRule](a,b) ++ manualGrounding

    val gp = LarsProgram(rules)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r1,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r1,v("Y")) == strVals("y"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("X")) == strVals("x","z"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("Y")) == strVals("y","z"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("X")) == strVals("x","z"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("Y")) == strVals("y","z"))

//    println(LarsProgram(grounder.groundProgram.rules))
//
//    val onlyInComputed = for (r <- grounder.groundProgram.rules if (!gp.rules.contains(r))) yield r
//    val onlyInExpected = for (r <- gp.rules if (!grounder.groundProgram.rules.contains(r))) yield r
//
//    println("only in computed: "+LarsProgram(onlyInComputed))
//    println("only in expected: "+LarsProgram(onlyInExpected))
//
//    printInspect(grounder)

    assert(grounder.groundProgram == gp)

    val model = modelFromClingo("a(x,y) b(y) i(x,y) i(z,z) j(x,y) j(z,z)")

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt13w") {

    val a = fact("a(x,y)")
    val b = fact("b(y)")

    val r1 = rule("i(X,Y) :- a(X,Y), b(Y), w_7_d_sig2(X,Y)")
    val r2 = rule("i(z,z) :- i(X,Y), w_7_d_sig1(X), not d(X,Y)")
    val r3 = rule("j(X,Y) :- i(X,Y), w_7_d_sig1(X), w_7_d_sig3(X,X,Y)")

    val p = program(a,b,r1,r2,r3)

    val manualGrounding: Set[LarsRule] = Set(
      rule("i(x,y) :- a(x,y), b(y), w_7_d_sig2(x,y)"),
      rule("i(z,z) :- i(x,y), w_7_d_sig1(x), not d(x,y)"),
      rule("i(z,z) :- i(z,z), w_7_d_sig1(z), not d(z,z)"),
      rule("i(z,z) :- i(z,y), w_7_d_sig1(z), not d(z,y)"),
      rule("i(z,z) :- i(x,z), w_7_d_sig1(x), not d(x,z)"),
      rule("j(z,y) :- i(z,y), w_7_d_sig1(z), w_7_d_sig3(z,z,y)"),
      rule("j(x,z) :- i(x,z), w_7_d_sig1(x), w_7_d_sig3(x,x,z)"),
      rule("j(x,y) :- i(x,y), w_7_d_sig1(x), w_7_d_sig3(x,x,y)"),
      rule("j(z,z) :- i(z,z), w_7_d_sig1(z), w_7_d_sig3(z,z,z)")
    )

    val rules = Seq[LarsRule](a,b) ++ manualGrounding

    val gp = LarsProgram(rules)
    val grounder = LarsGrounding(p)

    assert(grounder.inspect.possibleValuesForVariable(r1,v("X")) == strVals("x"))
    assert(grounder.inspect.possibleValuesForVariable(r1,v("Y")) == strVals("y"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("X")) == strVals("x","z"))
    assert(grounder.inspect.possibleValuesForVariable(r2,v("Y")) == strVals("y","z"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("X")) == strVals("x","z"))
    assert(grounder.inspect.possibleValuesForVariable(r3,v("Y")) == strVals("y","z"))

    //    println(LarsProgram(grounder.groundProgram.rules))
    //
    //    val onlyInComputed = for (r <- grounder.groundProgram.rules if (!gp.rules.contains(r))) yield r
    //    val onlyInExpected = for (r <- gp.rules if (!grounder.groundProgram.rules.contains(r))) yield r
    //
    //    println("only in computed: "+LarsProgram(onlyInComputed))
    //    println("only in expected: "+LarsProgram(onlyInExpected))
    //
    //    printInspect(grounder)

    assert(grounder.groundProgram == gp)

    //no semantics comparison with asp
  }

  test("gt rel 1") {
    /*
      % every machine can process only one task at a time
      :- assign(M,T1,P1), assign(M,T2,P2), neq(T1,T2), leq(P1,P2),
         duration(T1,D), plus(P1,D,Z), lt(P2,Z).
     */

    val r = rule("vals(M,T1,T2,P1,P2,D,Z) :- timepoint(Z), assign(M,T1,P1), assign(M,T2,P2), neq(T1,T2), leq(P1,P2), duration(T1,D), plus(P1,D,Z), lt(P2,Z)")

    val facts: Seq[LarsRule] = Seq(
      fact("assign(m1,t1,0)"),
      fact("assign(m2,t2,1)"),
      fact("assign(m2,t3,2)"),
      fact("duration(t1,4)"),
      fact("duration(t2,2)"),
      fact("duration(t3,3)"),
      fact("timepoint(0)"),
      fact("timepoint(1)"),
      fact("timepoint(2)"),
      fact("timepoint(3)"),
      fact("timepoint(4)"),
      fact("timepoint(5)"),
      fact("timepoint(6)"),
      fact("timepoint(7)"),
      fact("timepoint(8)"),
      fact("timepoint(9)"),
      fact("timepoint(10)")
    )

    assert(facts forall (_.head.isInstanceOf[GroundAtom]))

    val inputProgram = LarsProgram(facts ++ Seq(r))
    val larsGrounding = LarsGrounding(inputProgram)

    //println(larsGrounding.groundProgram)

    //
    // initial tests, variables to iterate over
    //

    val list0to10 = (for (i <- 0 to 10) yield ""+i).toList

    val valuesM = strVals("m1","m2")
    val valuesT1 = strVals("t1","t2","t3")
    val valuesT2 = valuesT1
    val valuesP1 = intVals("0","1","2")
    val valuesP2 = valuesP1
    val valuesZ = intVals(list0to10: _*)
    val valuesD = intVals("4","2","3")

    assert(larsGrounding.inspect.possibleValuesForVariable(r,v("M")) == valuesM)
    assert(larsGrounding.inspect.possibleValuesForVariable(r,v("T1")) == valuesT1)
    assert(larsGrounding.inspect.possibleValuesForVariable(r,v("T2")) == valuesT2)
    assert(larsGrounding.inspect.possibleValuesForVariable(r,v("P1")) == valuesP1)
    assert(larsGrounding.inspect.possibleValuesForVariable(r,v("P2")) == valuesP2)
    assert(larsGrounding.inspect.possibleValuesForVariable(r,v("Z")) == valuesZ)
    assert(larsGrounding.inspect.possibleValuesForVariable(r,v("D")) == valuesD)

    //
    //  craft expected ground program
    //

    //note that template does not include the auxiliary relation atoms!
    val template = "vals(M,T1,T2,P1,P2,D,Z) :- timepoint(Z), assign(M,T1,P1), assign(M,T2,P2), duration(T1,D)"

    val manualGrounding: Set[LarsRule] =
      for (m <- valuesM; t1 <- valuesT1; t2 <- valuesT2;
           p1 <- valuesP1; p2 <- valuesP2; z <- valuesZ; d <- valuesD
           if {
             t1 != t2 && asInt(p1) <= asInt(p2) && (asInt(p1) + asInt(d) == asInt(z)) && asInt(p2) < asInt(z)
           }
      ) yield {
        val str = template
          .replaceAll("M", m.toString)
          .replaceAll("T1", t1.toString)
          .replaceAll("T2", t2.toString)
          .replaceAll("P1", p1.toString)
          .replaceAll("P2", p2.toString)
          .replaceAll("D", d.toString)
          .replaceAll("Z", z.toString)

        rule(str)
      }

    val rules = facts ++ manualGrounding

    //println("#rules: "+rules.size)

    val gp = LarsProgram(rules)
//
//    val onlyInComputed = for (r <- larsGrounding.groundProgram.rules if (!gp.rules.contains(r))) yield r
//    val onlyInExpected = for (r <- gp.rules if (!larsGrounding.groundProgram.rules.contains(r))) yield r
//
//    println("only in computed: "+LarsProgram(onlyInComputed))
//    println("only in expected: "+LarsProgram(onlyInExpected))

    // printInspect(larsGrounding)

    assert(larsGrounding.groundProgram == gp)

    // ground rule that needs to fire:
    // from clingo:
    // vals(m2,t2,t3,1,2,2,3) :- 1+2=3, assign(m2,t2,1), 1<=2, duration(t2,2), 2<3, t2!=t3, timepoint(3), assign(m2,t3,2).
    val firingRule = rule("vals(m2,t2,t3,1,2,2,3) :- timepoint(3), assign(m2,t2,1), assign(m2,t3,2), duration(t2,2)")
    assert(larsGrounding.groundProgram.rules contains firingRule)

    val clingoModelStr =
      "assign(m1,t1,0) assign(m2,t2,1) assign(m2,t3,2) duration(t1,4) duration(t2,2) duration(t3,3) "+
        "timepoint(0) timepoint(1) timepoint(2) timepoint(3) timepoint(4) timepoint(5) timepoint(6) "+
        "timepoint(7) timepoint(8) timepoint(9) timepoint(10) vals(m2,t2,t3,1,2,2,3)"

    val model = modelFromClingo(clingoModelStr)

    val asp = asAspProgram(larsGrounding.groundProgram)
    val tms = jtmsInst(asp)
    assert(tms.getModel.get == model)
  }

  test("gt rel 2") {

    val r1 = rule("c(X,Y) :- a(X), b(Y), not d(X,Y), int(Z), plus(X,Y,Z), lt(Z,2)")
    val r2 = rule("d(X,Y) :- a(X), b(Y), not c(X,Y), int(Z), plus(X,Y,Z), lt(Z,2)")

    val facts: Seq[LarsRule] = Seq(
      fact("int(0)"),
      fact("int(1)"),
      fact("int(2)"),
      fact("a(0)"),
      fact("a(1)"),
      fact("b(0)"),
      fact("b(1)")
    )

    assert(facts forall (_.head.isInstanceOf[GroundAtom]))

    val inputProgram = LarsProgram(facts ++ Seq(r1,r2))
    val grounder = LarsGrounding(inputProgram)

    //println(grounder.groundProgram)

    //
    // initial tests, variables to iterate over
    //

    val valuesA = intVals("0","1")
    val valuesB = intVals("0","1")
    val valuesInt = intVals("0","1","2")

    Seq(r1,r2) foreach { r =>
      assert(grounder.inspect.possibleValuesForVariable(r,v("X")) == valuesA)
      assert(grounder.inspect.possibleValuesForVariable(r,v("Y")) == valuesB)
      assert(grounder.inspect.possibleValuesForVariable(r,v("Z")) == valuesInt)
    }


    //
    //  craft expected ground program
    //

    //note that template does not include the auxiliary relation atoms!
    val tmp1 = "c(X,Y) :- a(X), b(Y), not d(X,Y), int(Z)"
    val tmp2 = "d(X,Y) :- a(X), b(Y), not c(X,Y), int(Z)"

    val groupsOfGroundings: Set[Set[LarsRule]] =
      for (x <- (valuesA map asInt); y <- (valuesB map asInt); z <- (valuesInt map asInt)
           if {
             (x + y == z) && (z < 2)
           }
      ) yield {
        def replaceIn(template:String) = template
          .replaceAll("X", ""+x)
          .replaceAll("Y", ""+y)
          .replaceAll("Z", ""+z)

        Set(rule(replaceIn(tmp1)),rule(replaceIn(tmp2)))
      }

    val manualGrounding: Set[LarsRule] = groupsOfGroundings.flatten

    val rules = facts ++ manualGrounding

    //println("#rules: "+rules.size)
    //rules foreach { r => println(LarsProgram(Seq(r))) }

    val gp = LarsProgram(rules)
    //
//        val onlyInComputed = for (r <- grounder.groundProgram.rules if (!gp.rules.contains(r))) yield r
//        val onlyInExpected = for (r <- gp.rules if (!grounder.groundProgram.rules.contains(r))) yield r
//
//        println("only in computed: "+LarsProgram(onlyInComputed))
//        println("only in expected: "+LarsProgram(onlyInExpected))

    // printInspect(grounder)

    assert(grounder.groundProgram == gp)

    /* clingo models projected to c/2: */
    val clingoModelStrings = Set(
      "c(0,0) c(0,1) c(1,0)",
      "c(0,0) c(1,0)",
      "c(0,0) c(0,1)",
      "c(0,0)",
      "c(0,1) c(1,0)",
      "c(0,1)",
      "c(1,0)")

    val models = clingoModelStrings map modelFromClingo

    val asp = asAspProgram(grounder.groundProgram)
    val tms = jtmsInst(asp)
    val projectedModel = tms.getModel.get filter (_.predicate.caption == "c")
    //    println("projected model: "+projectedModel)

    assert(models contains projectedModel)
  }

}
