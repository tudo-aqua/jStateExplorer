package gov.nasa.jstateexplorer.transitionSystem;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.exceptions.ImpreciseRepresentationException;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.UnaryMinus;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionLabel;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import gov.nasa.jstateexplorer.transitionSystem.parser.TransitionSystemParser;
import java.util.Collection;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionLabelTest {
  
  private Variable x, y, xPrime, yPrime;
  private Constant c1, c5, c10, cneg1;
  
  @BeforeClass
  public void setUpMethod() throws Exception {
    x = new Variable(BuiltinTypes.SINT32, "x");
    y = new Variable(BuiltinTypes.SINT32, "y");
    xPrime = new Variable(BuiltinTypes.SINT32, "x'");
    yPrime = new Variable(BuiltinTypes.SINT32, "y'");
    
    c1 = new Constant(BuiltinTypes.SINT32, 1);
    c5 = new Constant(BuiltinTypes.SINT32, 5);
    c10 = new Constant(BuiltinTypes.SINT32, 10);
    cneg1 = new Constant(BuiltinTypes.SINT32, -1);
  }
  
  public TransitionLabelTest() {
  }

  @Test
  public void getCreateNewCollection(){
    TransitionLabel label = new TransitionLabel();
    Collection<Variable<?>> originalVars = label.getParameterVariables();
    assertTrue(originalVars.isEmpty());
    Variable var = new Variable(BuiltinTypes.SINT32, "x");
    originalVars.add(var);
    assertFalse(originalVars.isEmpty());
    assertTrue(label.getParameterVariables().isEmpty());

    NumericBooleanExpression expr = 
            new NumericBooleanExpression(var, NumericComparator.EQ, var);
    assertEquals(label.getPrecondition(), ExpressionUtil.TRUE);
    label.addPrecondition(expr);
    
    Expression originalPrecondition = label.getPrecondition();
    originalPrecondition = ExpressionUtil.TRUE;
    assertNotNull(originalPrecondition);
    assertNotNull(label.getPrecondition());
    assertNotEquals(label.getPrecondition(), originalPrecondition);
  }

  /*
  * This testcase makes sure, that ther precondition is included in the
  * effect clause.
  */
  @Test
  public void effectCalculationSimple() throws ImpreciseRepresentationException {
    String testSystem = "VARIABLES:\n"
            + "declare x:sint32, y:sint32\n"
            + "TRANSITION test:\n"
            + "PRECONDITION:\n"
            + "x < 5\n"
            + "EFFECT:\n"
            + "x: x' == 5\n"
            + "y: y' == 10 - x\n";
    
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(testSystem);
    TransitionLabel label = system.getTransitionLabelByName("test");
    Expression effectX = label.getEffectForVariable(x);
    Expression effectY = label.getEffectForVariable(y);
    
    NumericCompound additionEffectY = 
            NumericCompound.create(c10, NumericOperator.MINUS, x);
    NumericBooleanExpression effect = 
            new NumericBooleanExpression(
                    yPrime, NumericComparator.EQ, additionEffectY);
    NumericBooleanExpression precondition = 
            new NumericBooleanExpression(x, NumericComparator.LT, c5);
    PropositionalCompound expectedEffectY = 
            new PropositionalCompound(
                    precondition, LogicalOperator.AND, effect);
    assertEquals(effectY, expectedEffectY);
    
    NumericBooleanExpression expectedEffectX = 
            new NumericBooleanExpression(xPrime, NumericComparator.EQ, c5);
    assertEquals(effectX, expectedEffectX);
    
  }

  /*
  * This test ensures that also further restrictions on variables in the
  * precondition are keept.
  */
  @Test
  public void effectCalculationMoreComplexI() throws ImpreciseRepresentationException {
    String testSystem = "VARIABLES:\n"
            + "declare x:sint32, y:sint32\n"
            + "TRANSITION test:\n"
            + "PARAMETER:\n"
            + "declare p1:sint32\n"
            + "PRECONDITION:\n"
            + "x < p1\n"
            + "p1 == 5\n"
            + "y > 5\n"
            + "EFFECT:\n"
            + "x: x' == x + 1\n"
            + "y: y' == y\n";

    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(testSystem);
    TransitionLabel label = system.getTransitionLabelByName("test");
    
    Expression effectX = label.getEffectForVariable(x);
    
    Variable parameter = new Variable(BuiltinTypes.SINT32, "p1");
    
    NumericBooleanExpression part1 =
            new NumericBooleanExpression(x, NumericComparator.LT, parameter);
    NumericBooleanExpression part2 =
            new NumericBooleanExpression(parameter, NumericComparator.EQ, c5);
    NumericCompound part3 = new NumericCompound(x, NumericOperator.PLUS, c1);
    PropositionalCompound part4 = 
            new PropositionalCompound(part1, LogicalOperator.AND, part2);
    NumericBooleanExpression part5 =
            new NumericBooleanExpression(xPrime, NumericComparator.EQ, part3);
    PropositionalCompound expectedEffectX =
            new PropositionalCompound(part4, LogicalOperator.AND, part5);
    
    assertEquals(effectX, expectedEffectX);
  }

  /*
  * This test case ensures that precondition chains are also included in 
  * putting precondition parts together.
  */
  @Test
  public void effectCalculationMoreComplexII() throws ImpreciseRepresentationException {
    String testSystem = "VARIABLES:\n"
            + "declare x:sint32, y:sint32\n"
            + "TRANSITION test:\n"
            + "PARAMETER:\n"
            + "declare p1:sint32, p2:sint32\n"
            + "PRECONDITION:\n"
            + "p1 > p2\n" 
            + "x < p1\n"
            + "p2 == 5\n"
            + "y > 5\n"
            + "EFFECT:\n"
            + "x: x' == x + 1\n"
            + "y: y' == y\n";

    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(testSystem);
    TransitionLabel label = system.getTransitionLabelByName("test");
    
    Expression effectX = label.getEffectForVariable(x);
    
    Variable parameter1 = new Variable(BuiltinTypes.SINT32, "p1");
    Variable parameter2 = new Variable(BuiltinTypes.SINT32, "p2");
    
    NumericBooleanExpression part1 =
            new NumericBooleanExpression(x, NumericComparator.LT, parameter1);
    NumericBooleanExpression part2 =
            new NumericBooleanExpression(parameter1, 
                                         NumericComparator.GT, parameter2);
    NumericBooleanExpression part6 =
            new NumericBooleanExpression(parameter2, NumericComparator.EQ, c5);
    NumericCompound part3 = new NumericCompound(x, NumericOperator.PLUS, c1);
    PropositionalCompound part4 = 
            new PropositionalCompound(part1, LogicalOperator.AND, part2);
    PropositionalCompound part7 =
            new PropositionalCompound(part4, LogicalOperator.AND, part6);
    NumericBooleanExpression part5 =
            new NumericBooleanExpression(xPrime, NumericComparator.EQ, part3);
    PropositionalCompound expectedEffectX =
            new PropositionalCompound(part7, LogicalOperator.AND, part5);
    
    assertEquals(effectX, expectedEffectX);
  }

  /*
  * Whenever the prime variable is assigned to a constant,
  * historie might be removed.
  */
  @Test
  public void historieCutTest() throws ImpreciseRepresentationException {
    String inputSystem = "VARIABLES:\n"
            + "declare x:sint32\n"
            + "TRANSITION T1:\n"
            + "PARAMETER:\n"
            + "declare p:sint32\n"
            + "PRECONDITION:\n"
            + "x < p\n"
            + "EFFECT:\n"
            + "x: x' == 5";
    
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    TransitionLabel label = system.getTransitionLabelByName("T1");
    
    Expression effectX = label.getEffectForVariable(x);
    
    NumericBooleanExpression expectedEffectX = 
            new NumericBooleanExpression(xPrime, NumericComparator.EQ, c5);
    assertEquals(effectX, expectedEffectX);
  }
  
  @Test
  public void historieCutTestAssiginingNegativeValues() 
          throws ImpreciseRepresentationException{
    String inputSystem = "Variables:\n"
            + "declare x:sint32\n"
            + "Transition t1:\n"
            + "Precondition:\n"
            + "x >= 0\n"
            + "Effect:\n"
            + "x: x' == -1";
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    
    TransitionLabel label = system.getTransitionLabelByName("t1");
    Expression effectX = label.getEffectForVariable(x);
    
    Expression expectedEffect = new UnaryMinus(c1);
    expectedEffect = new NumericBooleanExpression(
            xPrime, NumericComparator.EQ, expectedEffect);
    assertEquals(effectX, expectedEffect);
  }
}
