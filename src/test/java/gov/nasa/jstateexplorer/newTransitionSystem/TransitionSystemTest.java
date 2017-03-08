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
            new NumericBooleanExpression(x, NumericComparator.EQ, c0);
    Expression valueB = 
            new NumericBooleanExpression(y, NumericComparator.EQ, c0);
    Expression expectedInitExpression = 
            new PropositionalCompound(valueA, LogicalOperator.AND, valueB);
    assertEquals(receivedInitState.toExpression(), expectedInitExpression);
  }

}
