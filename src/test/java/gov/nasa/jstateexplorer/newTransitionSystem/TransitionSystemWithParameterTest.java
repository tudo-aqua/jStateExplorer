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

  private Constant c200, c400;
  private Variable x, y, xPrime, yPrime;
  private Variable p1;

  public TransitionSystemWithParameterTest() {
  }
  
  @BeforeClass
  public void setupVariablesAndConstants(){
    c200 = new Constant(BuiltinTypes.SINT32, 200);
    c400 = new Constant(BuiltinTypes.SINT32, 400);
    
    x = new Variable(BuiltinTypes.SINT32, "x");
    y = new Variable(BuiltinTypes.SINT32, "y");
    xPrime = new Variable(BuiltinTypes.SINT32, "x'");
    yPrime = new Variable(BuiltinTypes.SINT32, "y'");
    p1 = new Variable(BuiltinTypes.SINT32, "p1");
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
  public void simpleTransitionSystemWithParameter() throws RecognitionException{
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
}
