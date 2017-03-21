package gov.nasa.jstateexplorer.transitionSystem.parser;

import gov.nasa.jstateexplorer.TestHelper;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import gov.nasa.jstateexplorer.newTransitionSystem.profiling.ProfiledTransitionSystem;
import org.antlr.runtime.RecognitionException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemParserProfiling {
  
  public TransitionSystemParserProfiling() {
  }

  @BeforeClass
  public void setupSolver(){
    TestHelper.setupSolver();
  }

  @Test
  public void setProfilingOnCreatesProfiledTransitionSystem() 
          throws RecognitionException {
    String input = "VARIABLES:\n"
            + "declare x:sint32\n"
            + "transition t1:\n"
            + "precondition:\n"
            + "x < 5\n"
            + "effect:\n"
            + "x: x' == x + 1";
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem normal = parser.parseString(input);
    
    parser = new TransitionSystemParser(true);
    TransitionSystem profiled = parser.parseString(input);
    assertEquals(profiled.getClass(), ProfiledTransitionSystem.class);
    int depthN = normal.unrollToFixPoint();
    int depthP = profiled.unrollToFixPoint();
    assertEquals(depthN, depthP);
    assertEquals(depthN,5);
    ProfiledTransitionSystem profile = (ProfiledTransitionSystem) profiled;
    // In case you want to have a look on a typical transition system profile
    // uncomment the following line and run this test.
    // System.out.println(profile.completeResultAsString());
  }
  
}
