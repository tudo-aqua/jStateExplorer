package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.TestHelper;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.transitionSystem.parser.TransitionSystemParser;
import java.util.ArrayList;
import java.util.List;
import org.antlr.runtime.RecognitionException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemWithDependingVariablesTest {
  private Variable x,y,xPrime,yPrime;
  private Constant c0, c5;
  public TransitionSystemWithDependingVariablesTest() {
  }

  @BeforeClass
  public void setUp(){
    x = new Variable(BuiltinTypes.SINT32, "x");
    xPrime = new Variable(BuiltinTypes.SINT32, "x'");
    yPrime = new Variable(BuiltinTypes.SINT32, "y'");
    y = new Variable(BuiltinTypes.SINT32, "y");
    
    c0 = new Constant(BuiltinTypes.SINT32, 0);
    c5 = new Constant(BuiltinTypes.SINT32, 5);

    TestHelper.setupSolver();
  }

  @Test
  public void testTransitionSystemWithDependingVariables() 
          throws RecognitionException{
    String testInput = "Variables:\n"
            + "declare x:sint32, y:sint32\n"
            + "TRANSITION t1:\n"
            + "PARAMETER:\n"
            + "declare p1:sint32\n"
            + "PRECONDITION:\n"
            + "p1 > 5\n"
            + "x <= 5\n"
            + "EFFECT:\n"
            + "x: x' == p1\n"
            + "Transition t5:\n"
            + "PRECONDITION:\n"
            + "x > 5\n"
            + "EFFECT:\n"
            + "x: x' == 0\n"
            + "y: y' == x";
    
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(testInput);
    
    system.initalize();
    int depth = system.unrollToFixPoint();
    
    assertEquals(depth, 4);
    
    TransitionLabel label = system.getTransitionLabelByName("t5");
    
    List<Transition> transitions = system.getTransitionsOfIteration(1);
    assertEquals(transitions.size(), 1);
    Transition transition1 = transitions.get(0);
    
    transitions = system.getTransitionsOfIteration(2);
    assertEquals(transitions.size(), 1);
    Transition transition2 = transitions.get(0);
    TransitionLabel executedLabel = 
            new ArrayList<>(
                    transition2.getTransitionLabels()).get(0);
    assertEquals(executedLabel, label);
    
    label = system.getTransitionLabelByName("t1");
    transitions = system.getTransitionsOfIteration(3);
    assertEquals(transitions.size(), 1);
    Transition transition3 = transitions.get(0);
    executedLabel = 
            new ArrayList<>(
                    transition3.getTransitionLabels()).get(0);
    assertEquals(executedLabel, label);
    
    SymbolicState state1 = transition1.getReachedState();
    SymbolicState state2 = transition2.getReachedState();
    SymbolicState state3 = transition3.getReachedState();
    
    Variable p1Replacement =
            new Variable(BuiltinTypes.SINT32, "p1_p_" + transition1.getID());
    Expression p1Constraint = 
            new NumericBooleanExpression(
                    p1Replacement, NumericComparator.GT, c5);
    
    Expression x1PreValue = 
            new NumericBooleanExpression(
                    x, NumericComparator.EQ, p1Replacement);
    Expression x1Value = ExpressionUtil.and(p1Constraint, x1PreValue);
    assertEquals(state1.get(x), x1Value);
    
    Expression y1Replacement = 
            new Variable(BuiltinTypes.SINT32, "y_sv_" + transition1.getID());
    Expression oldY0Value = 
            new NumericBooleanExpression(
                    y1Replacement, NumericComparator.EQ, c0);
    Expression y1Value = 
            new NumericBooleanExpression(
                    y, NumericComparator.EQ, y1Replacement);
    y1Value = ExpressionUtil.and(y1Value, oldY0Value);
    assertEquals(state1.get(y), y1Value);
    
    Expression x2Value = 
            new NumericBooleanExpression(x, NumericComparator.EQ, c0);
    assertEquals(state2.get(x), x2Value);

    Variable x2Replacement = 
            new Variable(BuiltinTypes.SINT32, "x_sv_" + transition2.getID());
    Expression x2Constraint = 
            new NumericBooleanExpression(
                    x2Replacement, NumericComparator.GT, c5);
    Expression y2PreValue = 
            new NumericBooleanExpression(
                    x2Replacement, NumericComparator.EQ, p1Replacement);
    y2PreValue = ExpressionUtil.and(p1Constraint,y2PreValue);
    Expression y2Value =
            new NumericBooleanExpression(
                    y, NumericComparator.EQ, x2Replacement);
    //Line order for addition to y2Value is important. Don't refactor!
    y2Value = ExpressionUtil.and(x2Constraint, y2Value);
    y2Value = ExpressionUtil.and(y2Value, y2PreValue);
    assertEquals(state2.get(y), y2Value);
    
    
    Variable p1Replacement3 =
            new Variable(BuiltinTypes.SINT32, "p1_p_" + transition3.getID());
    Expression p1Constraint3 = 
            new NumericBooleanExpression(
                    p1Replacement3, NumericComparator.GT, c5);
    
    Expression x3PreValue = 
            new NumericBooleanExpression(
                    x, NumericComparator.EQ, p1Replacement3);
    Expression x3Value = ExpressionUtil.and(p1Constraint3, x3PreValue);
    assertEquals(state3.get(x), x3Value);
    
    Expression y2Replacement = 
            new Variable(BuiltinTypes.SINT32, "y_sv_" + transition3.getID());
    Expression y3OldValue =
            new NumericBooleanExpression(
                    y2Replacement, NumericComparator.EQ, x2Replacement);
    //Line order for addition to y2Value is important. Don't refactor!
    y3OldValue = ExpressionUtil.and(x2Constraint, y3OldValue);
    y3OldValue = ExpressionUtil.and(y3OldValue, y2PreValue);
    Expression y3Value = 
            new NumericBooleanExpression(
                    y, NumericComparator.EQ, y2Replacement);
    y3Value = ExpressionUtil.and(y3Value, y3OldValue);
    assertEquals(state3.get(y), y3Value);
  }
}
