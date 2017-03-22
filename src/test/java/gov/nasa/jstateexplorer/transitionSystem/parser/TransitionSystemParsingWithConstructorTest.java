package gov.nasa.jstateexplorer.transitionSystem.parser;

import gov.nasa.jstateexplorer.TestHelper;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionLabel;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemParsingWithConstructorTest {
  
  public TransitionSystemParsingWithConstructorTest() {
  }

  @BeforeClass
  public void setUp(){
    TestHelper.setupSolver();
  }

  @Test
  public void parseConstructorTest() throws Exception {
    String input ="Variables\n"
            + "declare x:sint32, y:sint32\n"
            + "constructor t1\n"
            + "parameter:\n"
            + "declare p1:sint32\n"
            + "precondition\n"
            + "p1 < 5\n"
            + "effect:\n"
            + "x: x' == p1\n"
            + "y: y' == 3\n"
            + "constructor t2:\n"
            + "effect:\n"
            + "x: x' == 5\n"
            + "y: y' == 2\n"
            + "transition t3:\n"
            + "precondition:\n"
            + "x <= 3\n"
            + "effect:\n"
            + "x: x' == x +1";
  
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(input);
    TransitionLabel c1 = system.getTransitionLabelByName("t1");
    TransitionLabel c2 = system.getTransitionLabelByName("t2");
    TransitionLabel t3 = system.getTransitionLabelByName("t3");
    
    assertTrue(c1.isConstructor());
    assertTrue(c2.isConstructor());
    assertFalse(t3.isConstructor());
    
  }
}
