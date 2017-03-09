package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.UnaryMinus;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.TypeContext;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.SolverInstance;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.transitionSystem.parser.TransitionSystemParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import org.antlr.runtime.RecognitionException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemTest {
  
  private Constant c0, c1, c2, c5;
  private Variable x, y, xPrime, yPrime;
  
  public TransitionSystemTest() {
  }

  @BeforeClass
  public void setupVariablesAndConstants(){
    c1 = new Constant(BuiltinTypes.SINT32, 1);
    c5 = new Constant(BuiltinTypes.SINT32, 5);
    c2 = new Constant(BuiltinTypes.SINT32, 2);
    c0 = new Constant(BuiltinTypes.SINT32, 0);
    x = new Variable(BuiltinTypes.SINT32, "x");
    y = new Variable(BuiltinTypes.SINT32, "y");
    xPrime = new Variable(BuiltinTypes.SINT32, "x'");
    yPrime = new Variable(BuiltinTypes.SINT32, "y'");
  
    //Setup Z3 solver, as wee need it during enrolling of Transition Systems.
    Properties conf = new Properties();
    conf.setProperty("symbolic.dp", "NativeZ3");
    conf.setProperty("symbolic.dp.z3.bitvectors", "false");
    conf.setProperty("log.finest", "psyco");
    ConstraintSolverFactory factory = new ConstraintSolverFactory(conf);
    ConstraintSolver solver = factory.createSolver();
    SolverInstance.getInstance().setSolver(solver);
  }
  @Test
  public void getStateVariablesIsCopy(){
    TransitionSystem system = new TransitionSystem();
    Collection<Variable<?>> vars = system.getStateVariables();
    
    assertTrue(vars.isEmpty());
    
    Variable var = new Variable(BuiltinTypes.SINT16, "x");
    vars.add(var);
    assertNotEquals(vars, system.getStateVariables());
    system.addVariable(var);
    assertEquals(vars, system.getStateVariables());
  }

  @Test
  public void getTransitionLabelsIsCopy() {
    TransitionSystem system = new TransitionSystem();
    
    Collection<TransitionLabel> labels = system.getTransitionLabels();
    
    assertTrue(labels.isEmpty());
    
    TransitionLabel newLabel = new TransitionLabel("test");
    labels.add(newLabel);
    
    assertNotEquals(labels, system.getTransitionLabels());
    system.addTransitionLabel(newLabel);
    assertEquals(labels, system.getTransitionLabels());
    
    
  }

  @Test
  public void applyIterationTest() throws RecognitionException {
    String inputSystem = "VARIABLES:\n"
            + "declare x:sint32, y:sint32\n"
            + "TRANSITION h1:\n"
            + "PRECONDITION:\n"
            + "x >= 0\n"
            + "y < 5\n"
            + "EFFECT:\n"
            + "y: y' == y + 2\n"
            + "x: x' == -1\n"
            + "TRANSITION h2:\n"
            + "PRECONDITION:\n"
            + "x < 0\n"
            + "y >= 5\n"
            + "EFFECT:\n"
            + "y: y' == 0\n"
            + "x: x' == x + 1";
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    system.initalize();
    
    SymbolicState initState = system.getInitState();
    
    List<SymbolicState> lastReached = system.getStatesNewInDepth(0);
    assertFalse(lastReached.isEmpty());
    system.unrollToDepth(1);
    List<SymbolicState> states = system.getStatesNewInDepth(1);
    
    assertEquals(states.size(), 1);
    SymbolicState reachedState = states.get(0);

    List<Transition> transitions = system.getTransitionsOfIteration(1);
    TransitionLabel label = system.getTransitionLabelByName("h1");

    assertEquals(transitions.size(), 1);
    Transition appliedTransition = transitions.get(0);
    assertEquals(appliedTransition.getStartState(), initState);
    assertEquals(appliedTransition.getReachedState(), reachedState);
    HashSet expectedLabels = new HashSet();
    expectedLabels.add(label);
    assertEquals(appliedTransition.getTransitionLabels(),
            expectedLabels);
  
    Expression cneg1 = new UnaryMinus(c1);
    Expression xValue = 
            new NumericBooleanExpression(x, NumericComparator.EQ, cneg1);
    
    Expression yReplacement = 
            new Variable(BuiltinTypes.SINT32,
                    "y_sv_" + appliedTransition.getID());

    Expression yValuePart1 = 
            new NumericCompound(yReplacement, NumericOperator.PLUS, c2);
    yValuePart1 = 
            new NumericBooleanExpression(y, NumericComparator.EQ, yValuePart1);
    Expression yValuePart2 = 
            new NumericBooleanExpression(
                    yReplacement, NumericComparator.LT, c5);
    Expression yValuePart3 = 
            new NumericBooleanExpression(
                    yReplacement, NumericComparator.EQ, c0);
    Expression yValue = 
            ExpressionUtil.and(yValuePart2, yValuePart1);
    yValue = ExpressionUtil.and(yValue, yValuePart3);
    assertEquals(reachedState.get(x), xValue);
    assertEquals(reachedState.get(y), yValue);
  }

  @Test
  public void applyIterationTillEnd1Test() throws RecognitionException {
    String inputSystem = "VARIABLES:\n"
            + "declare x:sint32, y:sint32\n"
            + "TRANSITION h1:\n"
            + "PRECONDITION:\n"
            + "x >= -1\n"
            + "y < 5\n"
            + "EFFECT:\n"
            + "y: y' == y + 2\n"
            + "x: x' == -1\n"
            + "TRANSITION h2:\n"
            + "PRECONDITION:\n"
            + "x < 0\n"
            + "y >= 5\n"
            + "EFFECT:\n"
            + "y: y' == 0\n"
            + "x: x' == x + 1";
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    system.initalize();
    
    int fixPointDepth = system.unrollToFixPoint();
    //system.unrollToDepth(10);
    assertEquals(fixPointDepth, 3);
    assertEquals(system.getStatesNewInDepth(2).size(), 1);
    assertEquals(system.getStatesNewInDepth(3).size(), 1);
    assertEquals(system.getStatesNewInDepth(4).size(), 0);
    
    Expression cneg1 = new UnaryMinus(c1);
    Expression xValue = 
            new NumericBooleanExpression(x, NumericComparator.EQ, cneg1);
    
    Transition appliedTransition1 = system.getTransitionsOfIteration(1).get(0);
    assertEquals(system.getTransitionsOfIteration(2).size(), 1);
    Transition appliedTransition2 = system.getTransitionsOfIteration(2).get(0);
    assertEquals(system.getTransitionsOfIteration(3).size(), 1);
    Transition appliedTransition3 = system.getTransitionsOfIteration(3).get(0);
    assertEquals(system.getTransitionsOfIteration(4).size(), 1);
    Transition appliedTransition4 = system.getTransitionsOfIteration(4).get(0);

    SymbolicState newDepth2 = system.getStatesNewInDepth(2).get(0);
    SymbolicState newDepth3 = system.getStatesNewInDepth(3).get(0);
    SymbolicState newDepth4 = appliedTransition4.getReachedState();


    
    Expression effect2X = newDepth2.get(x);
    assertEquals( effect2X, xValue);
    
    Expression effect3X = newDepth3.get(x);
    assertEquals(effect3X, xValue);
    
    Expression effect4X = newDepth4.get(x);
    Variable xReplacement4 = new Variable(
            BuiltinTypes.SINT32, "x_sv_" + appliedTransition4.getID());
    Expression effect4XPart1 = new NumericBooleanExpression(
            xReplacement4, NumericComparator.EQ, cneg1);
    Expression effect4XPart2 = new NumericBooleanExpression(
            xReplacement4, NumericComparator.LT, c0);
    Expression effect4Xexpected = 
            new NumericCompound(xReplacement4, NumericOperator.PLUS, c1);
    effect4Xexpected =
            new NumericBooleanExpression(
                    x, NumericComparator.EQ, effect4Xexpected);
    effect4Xexpected = ExpressionUtil.and(effect4XPart2, effect4Xexpected);
    effect4Xexpected = ExpressionUtil.and(effect4Xexpected, effect4XPart1);
    assertEquals(effect4X, effect4Xexpected);
    
    Expression yReplacement1 = 
            new Variable(BuiltinTypes.SINT32,
                    "y_sv_" + appliedTransition1.getID());
    Expression yReplacement2 =
            new Variable(BuiltinTypes.SINT32,
                    "y_sv_" + appliedTransition2.getID());
    Expression yReplacement3 =
            new Variable(BuiltinTypes.SINT32,
                    "y_sv_" + appliedTransition3.getID());
    //yValue after depth 1
    Expression yValuePart1pre = 
            new NumericCompound(yReplacement1, NumericOperator.PLUS, c2);
    Expression yValuePart1 = 
            new NumericBooleanExpression(
                    y, NumericComparator.EQ, yValuePart1pre);
    Expression yValuePart2 = 
            new NumericBooleanExpression(
                    yReplacement1, NumericComparator.LT, c5);
    Expression yValuePart3 = 
            new NumericBooleanExpression(
                    yReplacement1, NumericComparator.EQ, c0);
    Expression yValue1 = 
            ExpressionUtil.and(yValuePart2, yValuePart1);
    yValue1 = ExpressionUtil.and(yValue1, yValuePart3);
    
    //yValue after Depth 2
    // (y_sv_2 +2)
    Expression yValue2Part1pre = 
            new NumericCompound(yReplacement2, NumericOperator.PLUS, c2);
    //
    Expression yValue2Part1 =
            new NumericBooleanExpression(
                    y, NumericComparator.EQ, yValue2Part1pre);
    Expression yValue2Part2 =
            new NumericBooleanExpression(
                    yReplacement2, NumericComparator.LT, c5);
    Expression yValue2Part4 = ExpressionUtil.and(yValue2Part2, yValue2Part1);
    Expression yValue2Part3 = 
            new NumericBooleanExpression(
                    yReplacement2, NumericComparator.EQ, yValuePart1pre);
    yValue2Part3 = ExpressionUtil.and(yValuePart2, yValue2Part3);
    yValue2Part3 = ExpressionUtil.and(yValue2Part3, yValuePart3);
    Expression yValue2 = ExpressionUtil.and(yValue2Part4, yValue2Part3);
    Expression effectY2 = newDepth2.get(y);
    assertEquals(effectY2, yValue2);

    //yValue after Depth 3
    //(y_sv_4 +2)
    Expression yValue3Part1Pre =
            new NumericCompound(yReplacement3, NumericOperator.PLUS, c2);
    //y == (y_sv_4 + 2)
    Expression yValue3Part1 =
            new NumericBooleanExpression(
                    y, NumericComparator.EQ, yValue3Part1Pre);
    //(y_sv_4 < 5)
    Expression yValue3Part2 =
            new NumericBooleanExpression(
                    yReplacement3, NumericComparator.LT, c5);
    //(y_sv_4 < 5) && y == (y_sv_4 + 2)
    yValue3Part2 = ExpressionUtil.and(yValue3Part2, yValue3Part1);
    // y_sv_4 == (y_sv_3 + 2)
    Expression yValue3Part3 = 
            new NumericBooleanExpression(
                    yReplacement3, NumericComparator.EQ, yValue2Part1pre);
    //(y_sv_3 <5) && y_sv_4 == (y_sv_3 + 2)
    yValue3Part3 = ExpressionUtil.and(yValue2Part2, yValue3Part3);
    yValue3Part3 = ExpressionUtil.and(yValue3Part3, yValue2Part3);
    Expression yValue3 = ExpressionUtil.and(yValue3Part2, yValue3Part3);
    Expression effectY3 = newDepth3.get(y);
    assertEquals(effectY3, yValue3);
    
    //yVlaue after Depth 4
    Expression yValue4 = 
            new NumericBooleanExpression(y, NumericComparator.EQ, c0);
    Expression effectY4 = newDepth4.get(y);
    assertEquals(effectY4, yValue4);
  }

  @Test
  public void getInitStateWithBuiltinTypes() {
    List<Variable<?>> stateVariables = new ArrayList<>();

    stateVariables.add(x);
    stateVariables.add(y);
    TransitionSystem system = new TransitionSystem();
    system.addVariables(stateVariables);
    
    SymbolicState initState = new SymbolicState();
    initState.setToStateVariables(stateVariables, new TypeContext(true));
    
    system.setInitState(initState);
    SymbolicState receivedInitState = system.getInitState();
    assertNotNull(receivedInitState);
    
    Constant c0 = new Constant(BuiltinTypes.SINT32, 0);
    Expression valueA = 
            new NumericBooleanExpression(y, NumericComparator.EQ, c0);
    Expression valueB = 
            new NumericBooleanExpression(x, NumericComparator.EQ, c0);
    Expression expectedInitExpression = 
            new PropositionalCompound(valueA, LogicalOperator.AND, valueB);
    assertEquals(receivedInitState.toExpression(), expectedInitExpression);
  }
  
  @Test
  public void reachesErrorStateTest() throws RecognitionException {
    String inputSystem = "Variables:\n"
            + "declare x:sint32\n"
            + "Transition t1:\n"
            + "EFFECT:\n"
            + "ERROR\n";
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    
    system.initalize();
    system.unrollToFixPoint();
    
    assertEquals(system.getTransitionsOfIteration(1).size(), 1);
    Transition appliedTransition = system.getTransitionsOfIteration(1).get(0);
    assertTrue(appliedTransition.hasReachedErrorState());
    SymbolicState errorState = appliedTransition.getReachedState();
    assertTrue(errorState.isError());
  }

}
