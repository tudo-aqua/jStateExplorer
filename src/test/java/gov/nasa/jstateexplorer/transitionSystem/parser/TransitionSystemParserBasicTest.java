package gov.nasa.jstateexplorer.transitionSystem.parser;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.casts.CastOperation;
import gov.nasa.jpf.constraints.expressions.CastExpression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionLabel;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.antlr.runtime.RecognitionException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemParserBasicTest {
  
  TransitionSystemParser parser;
  Variable x, b,c;
  public TransitionSystemParserBasicTest() {
  }

  @Test
  public void testVariableDeclaration() throws RecognitionException {
    parser = new TransitionSystemParser();
    String testFile = "VARIABLES\n"
            +"declare x:sint32, b:bool, c:sint8\n";
    
    TransitionSystem system = parser.parseString(testFile);
    List<Variable<?>> stateVariables = system.getStateVariables();
    assertTrue(stateVariables.contains(x));
    assertTrue(stateVariables.contains(b));
    assertTrue(stateVariables.contains(c));
  }

  @Test
  public void testTransitionDeclaration() throws RecognitionException{
    parser = new TransitionSystemParser();
    
    String testSnippet = "VARIABLES\n"
            + "declare x:sint32\n"
            + "TRANSITION h1:\n"
            + "This is a very nice test transition.\n"
            + "PRECONDITION:\n"
            + "x > 5\n"
            + "EFFECT:\n"
            + "x: x' == 5\n";
            
    TransitionSystem system = parser.parseString(testSnippet);
    List<Variable<?>> stateVariable = system.getStateVariables();
    assertTrue(stateVariable.contains(x));
    List<TransitionLabel> transitionLabels = system.getTransitionLabels();
    assertEquals(transitionLabels.size(), 1);
    TransitionLabel createdTransitionLabel = transitionLabels.get(0);
    assertEquals(createdTransitionLabel.getName(), "h1");
    
    Constant c5 = new Constant(BuiltinTypes.SINT32, 5);
    NumericBooleanExpression precondition = 
            new NumericBooleanExpression(x, NumericComparator.GT, c5);
    assertEquals(createdTransitionLabel.getPrecondition(), precondition);
    Variable xprime = new Variable(BuiltinTypes.SINT32, "x'");
    
    //As the precondition doesn't influence the Effects,
    //it should not be included in the effect clause.
    //We will have other test cases to assure this in other cases, but as this
    //is not the functionality of TransitionSystemParser this testcases are
    //not included here.
    NumericBooleanExpression effect = 
            new NumericBooleanExpression(xprime, NumericComparator.EQ, c5);
    Expression<Boolean> completeEffect = 
            ExpressionUtil.and(precondition, effect);
    assertEquals(createdTransitionLabel.getEffect(), completeEffect);
    assertEquals(createdTransitionLabel.getEffectForVariable(x), effect);
  }

  @Test
  public void parsingTwoTransition() throws RecognitionException {
    parser = new TransitionSystemParser();

    String input ="VARIABLES:\n"
            + "declare a:sint32, b:sint32\n"
            + "Transition h1:\n"
            + "PRECONDITION\n"
            + "a < 5\n"
            + "EFFECTS:\n"
            + "b: b' == 5\n"
            + "Transition h2: \n"
            + "EFFECT\n"
            + "a: a' == 10";
    TransitionSystem system = parser.parseString(input);
    assertEquals(system.getTransitionLabels().size(), 2);
    HashSet<String> names = new HashSet<>();
    names.add("h1");
    names.add("h2");
    for(TransitionLabel label: system.getTransitionLabels()){
      names.remove(label.getName());
    }
    assertTrue(names.isEmpty());
    
    TransitionLabel label = system.getTransitionLabelByName("h2");
    assertEquals(label.getPrecondition(), ExpressionUtil.TRUE);
    assertNotEquals(label.getEffect(), ExpressionUtil.TRUE);
    
    label = system.getTransitionLabelByName("h1");
    Variable b = new Variable(BuiltinTypes.SINT32, "b");
    Variable bprime = new Variable(BuiltinTypes.SINT32, "b'");
    Expression parsedEffect = label.getEffectForVariable(b);
    Expression expectedEffect = 
            new NumericBooleanExpression(
                    bprime, 
                    NumericComparator.EQ,
                    new Constant(BuiltinTypes.SINT32, 5));
    assertEquals(parsedEffect, expectedEffect);
   
  }
  
  @Test
  public void readTransitionSystemFromFile() 
          throws IOException, FileNotFoundException, RecognitionException {
    parser = new TransitionSystemParser();
    
    String fileName = 
            "src/resources/testInputTransitionSystem/" 
            + "transitionSystem.symbolicts";
    
    TransitionSystem system = parser.parseFile(fileName);

    Collection<Variable<?>> stateVariables = system.getStateVariables();
    assertTrue(stateVariables.contains(x));
    assertTrue(stateVariables.contains(b));
    assertTrue(stateVariables.contains(c));
    
    TransitionLabel label = system.getTransitionLabelByName("t1");
    assertNotNull(label);
    
    Variable xPrime = new Variable(x.getType(), "x'");
    Expression xEffect = 
            new NumericBooleanExpression(xPrime, NumericComparator.EQ, x);
    assertNotEquals(label.getEffectForVariable(x), xEffect);
    
    Variable bPrime = new Variable(b.getType(), "b'");
    Expression bEffect = 
            new NumericBooleanExpression(
                    bPrime, NumericComparator.EQ, ExpressionUtil.FALSE);
    assertEquals(label.getEffectForVariable(b), bEffect);
    
  }
  
  @Test
  public void transitionWithParameter() throws RecognitionException {
    String inputFile = "VARIABLES\n"
            + "declare x:sint32\n"
            + "TRANSITION h1:\n"
            + "This is a very nice test transition.\n"
            + "PARAMETER:\n"
            + "declare p1:sint32, p2:sint32\n"
            + "PRECONDITION:\n"
            + "x > p1\n"
            + "p2 < 5\n"
            + "EFFECT:\n"
            + "x: x' == p1\n";
    
    parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputFile);
    TransitionLabel label = system.getTransitionLabelByName("h1");
    assertNotNull(label);
    
    List<Variable <?>> parameters = label.getParameterVariables();
    Variable p1 = new Variable(BuiltinTypes.SINT32, "p1");
    Variable p2 = new Variable(BuiltinTypes.SINT32, "p2");
    
    assertTrue(parameters.contains(p1));
    assertTrue(parameters.contains(p2));
    assertEquals(parameters.get(0), p1);
    assertEquals(parameters.get(1), p2);
  }
  
  @Test
  public void errorTransition() throws RecognitionException {
    String inputSystem = "VARIABLES:\n"
            + "declare x:sint32\n"
            + "TRANSITION T1:\n"
            + "PRECONDITION:\n"
            + "x > 5\n"
            + "EFFECT:\n"
            + "ERROR\n"
            + "TRANSITION T2:\n"
            + "EFFECT:\n"
            + "x: x' == x + 1\n";
    
    parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    
   TransitionLabel label = system.getTransitionLabelByName("T1");
   assertTrue(label.isError());
   label = system.getTransitionLabelByName("T2");
   assertFalse(label.isError());
  }

  @Test
  public void mostSimpleErrorTransitionTest() throws RecognitionException {
    String inputSystem = "Variables\n"
            + "declare x:sint32\n"
            + "Transition t1:\n"
            + "EFFECT:\n"
            + "ERROR\n";
    parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(inputSystem);
    TransitionLabel errorLabel = system.getTransitionLabelByName("t1");
    assertTrue(errorLabel.isError());
  }
  @BeforeMethod
  public void setUpMethod() throws Exception {
    x = new Variable(BuiltinTypes.SINT32, "x");
    b = new Variable(BuiltinTypes.BOOL, "b");
    c = new Variable(BuiltinTypes.SINT8, "c");
  }
}
