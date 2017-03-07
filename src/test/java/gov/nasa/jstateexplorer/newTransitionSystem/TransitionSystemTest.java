package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.TypeContext;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemTest {
  
  public TransitionSystemTest() {
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
    
    //Start Debug
    for(SymbolicState state: states){
      System.out.println("STatE: " + state.toExpression());
    }

    //End Debug
    
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
  }
  
  @Test
  public void getInitStateWithBuiltinTypes() {
    List<Variable<?>> stateVariables = new ArrayList<>();
    Variable a = new Variable(BuiltinTypes.SINT32, "a");
    Variable b = new Variable(BuiltinTypes.SINT32, "b");

    stateVariables.add(a);
    stateVariables.add(b);
    TransitionSystem system = new TransitionSystem();
    system.addVariables(stateVariables);
    
    SymbolicState initState = new SymbolicState();
    initState.setToStateVariables(stateVariables, new TypeContext(true));
    
    system.setInitState(initState);
    SymbolicState receivedInitState = system.getInitState();
    assertNotNull(receivedInitState);
    
    Constant c0 = new Constant(BuiltinTypes.SINT32, 0);
    Expression valueA = new NumericBooleanExpression(a, NumericComparator.EQ, c0);
    Expression valueB = new NumericBooleanExpression(b, NumericComparator.EQ, c0);
    Expression expectedInitExpression = new PropositionalCompound(valueA, LogicalOperator.AND, valueB);
    assertEquals(receivedInitState.toExpression(), expectedInitExpression);
  }

  @BeforeMethod
  public void setUpMethod() throws Exception {
    Properties conf = new Properties();
    conf.setProperty("symbolic.dp", "NativeZ3");
    conf.setProperty("symbolic.dp.z3.bitvectors", "false");
    conf.setProperty("log.finest", "psyco");
    ConstraintSolverFactory factory = new ConstraintSolverFactory(conf);
    ConstraintSolver solver = factory.createSolver();
    SolverInstance.getInstance().setSolver(solver);
  }
}
