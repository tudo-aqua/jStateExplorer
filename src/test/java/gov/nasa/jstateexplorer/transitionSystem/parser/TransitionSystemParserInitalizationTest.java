package gov.nasa.jstateexplorer.transitionSystem.parser;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.exceptions.ImpreciseRepresentationException;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.UnaryMinus;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemParserInitalizationTest {
  
  private Variable x, t, y, z;
  private Constant c5, c0, c3;
  Expression neg3;
  public TransitionSystemParserInitalizationTest() {
    x = new Variable(BuiltinTypes.SINT32, "x");
    y = new Variable(BuiltinTypes.BOOL, "y");
    t = new Variable(BuiltinTypes.SINT32, "t");
    z = new Variable(BuiltinTypes.SINT32, "z");
    
    c0 = new Constant(BuiltinTypes.SINT32, 0);
    c3 = new Constant(BuiltinTypes.SINT32, 3);
    c5 = new Constant(BuiltinTypes.SINT32, 5);
    neg3 = new UnaryMinus(c3);
  }

  @Test
  public void transitionSystemWithInitValues() 
          throws ImpreciseRepresentationException {
    String inputSystem = "VARIABLES:\n"
            + "declare t:sint32, x:sint32, y:bool, z:sint32\n"
            + "INIT:\n"
            + "x: x == 5\n"
            + "y: y == true\n"
            + "t: t == -3\n";
    
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    system.initalize();
    assertEquals(system.getInitState().size(), 1);
    SymbolicState init = system.getInitState().get(0);
    
    Expression xValue = 
            new NumericBooleanExpression(x, NumericComparator.EQ, c5);
    Expression yValue = 
            new PropositionalCompound(
                    y, LogicalOperator.EQUIV, ExpressionUtil.TRUE);
    Expression tValue = 
            new NumericBooleanExpression(t, NumericComparator.EQ, neg3);
    Expression zValue = 
            new NumericBooleanExpression(z, NumericComparator.EQ, c0);
    
    assertEquals(init.get(x), xValue);
    assertEquals(init.get(t), tValue);
    assertEquals(init.get(z), zValue);
    assertEquals(init.get(y), yValue);
  }

}
