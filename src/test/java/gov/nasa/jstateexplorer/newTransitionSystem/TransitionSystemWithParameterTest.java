package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.SolverInstance;
import gov.nasa.jstateexplorer.TestHelper;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.transitionSystem.parser.TransitionSystemParser;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.antlr.runtime.RecognitionException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemWithParameterTest {

  private Constant c200, c400, c50;
  private Variable x, y, xPrime, yPrime;
  private Variable p1;

  public TransitionSystemWithParameterTest() {
  }
  
  @BeforeClass
  public void setupVariablesAndConstants(){
    c50 = new Constant(BuiltinTypes.SINT32, 50);
    c200 = new Constant(BuiltinTypes.SINT32, 200);
    c400 = new Constant(BuiltinTypes.SINT32, 400);
    
    x = new Variable(BuiltinTypes.SINT32, "x");
    y = new Variable(BuiltinTypes.SINT32, "y");
    xPrime = new Variable(BuiltinTypes.SINT32, "x'");
    yPrime = new Variable(BuiltinTypes.SINT32, "y'");
    p1 = new Variable(BuiltinTypes.SINT32, "p1");
    TestHelper.setupSolver();
  }

  @Test
  public void simpleTransitionSystemWithParameter1() throws RecognitionException, RecognitionException, RecognitionException{
    String testInput = "Variables:\n"
            + "declare x:sint32\n"
            + "TRANSITION t1:\n"
            + "PARAMETER:\n"
            + "declare p1:sint32\n"
            + "PRECONDITION: \n"
            + "p1 > 200 && p1 < 400 \n"
            + "EFFECT:\n"
            + "x: x' == p1\n"
            + "Transition t2:\n"
            + "PARAMETER:\n"
            + "declare p1:sint32\n"
            + "PRECONDITION:\n"
            + "p1 <= 200 || p1 >= 400\n"
            + "EFFECT:\n"
            + "ERROR";
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(testInput);
    
    system.initalize();
    int depth = system.unrollToFixPoint();
    List<SymbolicState> allStatesInDepth1 = system.getAllStatesInDepth(1);
    assertTrue(allStatesInDepth1.contains(system.getErrorState()));
    
    assertEquals(depth, 1);
    
    assertEquals(system.getStatesNewInDepth(1).size(), 2);
    List<Transition> transitionsInDepth1 = system.getTransitionsOfIteration(1);
    assertEquals(transitionsInDepth1.size(), 2);
    
    Transition transition1 = transitionsInDepth1.get(0);
    Transition transition2 = transitionsInDepth1.get(1);
    TransitionLabel t1 = system.getTransitionLabelByName("t1");
    Set<TransitionLabel> labelsInTransition1 = 
            transition1.getTransitionLabels();
    //A consumption necessary for the test. 
    //Might be changed, if enrollment strategie is changed.
    assertTrue(labelsInTransition1.contains(t1));
    
    SymbolicState resultT1 = transition1.getReachedState();
    
    Variable p1Replacement = new Variable(p1.getType(),
            "p1_p_" + transition1.getID());
    Expression preconditionXPart1 = 
            new NumericBooleanExpression(
                    p1Replacement, NumericComparator.GT, c200);
    Expression preconditionXPart2 =
            new NumericBooleanExpression(
                    p1Replacement, NumericComparator.LT, c400);
    Expression preconditionX = 
            ExpressionUtil.and(preconditionXPart1, preconditionXPart2);
    
    Expression effectX = 
            new NumericBooleanExpression(
                    x, NumericComparator.EQ, p1Replacement);
    effectX = ExpressionUtil.and(preconditionX, effectX);
    assertEquals(resultT1.get(x), effectX);
    
    assertTrue(transition2.hasReachedErrorState());
    SymbolicState resultT2 = transition2.getReachedState();
    assertTrue(resultT2.isError());
  }
  
  @Test
  public void simpleTransitionSystemWithParameter2()
          throws RecognitionException {
    String testInput = "Variables:\n"
            + "declare x:sint32\n"
            + "TRANSITION t1:\n"
            + "PARAMETER:\n"
            + "declare p1:sint32\n"
            + "PRECONDITION: \n"
            + "p1 > 200 && p1 < 400 \n"
            + "EFFECT:\n"
            + "x: x' == p1\n"
            + "Transition t2:\n"
            + "PARAMETER:\n"
            + "declare p1:sint32\n"
            + "PRECONDITION:\n"
            + "x == 300\n"
            + "EFFECT:\n"
            + "ERROR\n";
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(testInput);
    
    system.initalize();
    int depth = system.unrollToFixPoint();
    assertEquals(depth, 2);

    List<SymbolicState> newStatesInDepth1 = system.getStatesNewInDepth(2);
    assertEquals(newStatesInDepth1.size(), 1);
    
    List<SymbolicState> newStatesInDepth2 = system.getStatesNewInDepth(2);
    assertEquals(newStatesInDepth2.size(), 1);
    assertEquals(newStatesInDepth2.get(0), system.getErrorState());
  }
  @Test
  public void simpleTransitionSystemWithParameter3() 
          throws RecognitionException {
    String testInput = "Variables:\n"
          + "declare x:sint32\n"
          + "TRANSITION t1:\n"
          + "PARAMETER:\n"
          + "declare p1:sint32\n"
          + "PRECONDITION: \n"
          + "p1 > 200 && p1 < 400 \n"
          + "EFFECT:\n"
          + "x: x' == p1\n"
          + "Transition t2:\n"
          + "PRECONDITION:\n"
          + "x == 100\n"
          + "EFFECT:\n"
          + "ERROR\n"
          + "Transition t5:\n"
          + "PRECONDITION:\n"
          + "x >= 400\n"
          + "EFFECT\n"
          + "ERROR\n"
          + "Transition t3:\n"
          + "PRECONDITION:\n"
          + "x >= 399\n"
          + "EFFECT:\n"
          + "x: x' == 50\n"
          + "Transition t4:\n"
          + "PRECONDITION:\n"
          + "x < 53 && x != 0\n"
          + "EFFECT:\n"
          + "x: x' == x +1";

    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(testInput);
    
    system.initalize();
    int depth = system.unrollToFixPoint();
    assertEquals(depth, 5);
    
    List<SymbolicState> newStatesInDepth2 = system.getStatesNewInDepth(2);
    assertEquals(newStatesInDepth2.size(), 1);
    
    Expression xValue = 
            new NumericBooleanExpression(x, NumericComparator.EQ, c50);
    assertEquals(newStatesInDepth2.get(0).get(x), xValue);
    
    TransitionLabel unreachedLabel = system.getTransitionLabelByName("t2");
    assertFalse(unreachedLabel.isExecuted());
    
    TransitionLabel t5 = system.getTransitionLabelByName("t5");
    List<SymbolicState> newStatesInDepth1 = system.getStatesNewInDepth(1);
    SymbolicState stateDepth1 = newStatesInDepth1.get(0);
    assertFalse(t5.isEnabledOnState(stateDepth1));
    
    assertFalse(system.getErrorState().hasIncomingTransitions());
  }
}
