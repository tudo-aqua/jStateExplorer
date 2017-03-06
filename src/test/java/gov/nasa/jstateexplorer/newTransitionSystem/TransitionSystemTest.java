package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.Collection;
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
  @BeforeMethod
  public void setUpMethod() throws Exception {
  }
}
