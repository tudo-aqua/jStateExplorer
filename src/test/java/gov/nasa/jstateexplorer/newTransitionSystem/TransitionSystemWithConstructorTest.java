package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jstateexplorer.TestHelper;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.transitionSystem.parser.TransitionSystemParser;
import java.util.List;
import org.antlr.runtime.RecognitionException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemWithConstructorTest {
  
  public TransitionSystemWithConstructorTest() {
  }

  @BeforeClass
  public void setupSolver() {
    TestHelper.setupSolver();
  }
  
  @Test
  public void constructorsAreExecutedOnlyOnce() throws RecognitionException{
    String input = "Variables:\n"
            + "declare x:sint32\n"
            + "transition t1: \n"
            + "precondition:\n"
            + "x < 3 \n"
            + "effect:\n"
            + "x: x' == x + 1\n"
            + "transition t2:\n"
            + "precondition:\n"
            + "x > 7\n"
            + "x < 10\n"
            + "effect:\n"
            + "x: x' == x - 1\n"
            + "constructor c1:\n"
            + "effect:\n"
            + "x: x' == 0\n"
            + "constructor c2:\n"
            + "effect:\n"
            + "x: x' == 9";
    
    TransitionSystemParser parser = new TransitionSystemParser();
    TransitionSystem system = parser.parseString(input);
    
    system.initalize();
    int depth = system.unrollToFixPoint();
    
    assertEquals(depth, 4);

    List<SymbolicState> initStates = system.getInitState();
    assertEquals(initStates.size(), 2);
    
    SymbolicState state0 = initStates.get(0);
    SymbolicState state1 = initStates.get(1);
    
    List<Transition> initTransitions = state0.getIncommingTransitions();
    assertEquals(initTransitions.size(), 1);
    Transition initTransition0 = initTransitions.get(0);
    assertEquals(initTransition0.getTransitionLabels().size(), 1);
    TransitionLabel c1 = 
            (TransitionLabel) initTransition0.
                    getTransitionLabels().toArray()[0];
    assertEquals(c1.getName(), "c1");
    
    assertEquals(c1.getExecutingTransitions().size(), 1);
    Transition exec1 = c1.getExecutingTransitions().get(0);
    assertEquals(exec1, initTransition0);
    
    initTransitions = state1.getIncommingTransitions();
    assertEquals(initTransitions.size(), 1);
    Transition initTransition1 = initTransitions.get(0);
    assertEquals(initTransition1.getTransitionLabels().size(), 1);
    TransitionLabel c2 = 
            (TransitionLabel) initTransition1.
                    getTransitionLabels().toArray()[0];
    assertEquals(c2.getName(), "c2");

    assertEquals(c2.getExecutingTransitions().size(), 1);
    Transition exec2 = c2.getExecutingTransitions().get(0);
    assertEquals(exec2, initTransition1);
  
    //There should be 2 new states for the first two iterations.
    assertEquals(system.getAllStatesInDepth(1).size(), 2);
    assertEquals(system.getAllStatesInDepth(2).size(), 2);
    assertEquals(system.getAllStatesInDepth(3).size(), 1);
  }

}
