package gov.nasa.jstateexplorer.newTransitionSystem.profiling;


import gov.nasa.jstateexplorer.newTransitionSystem.TransitionLabel;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import gov.nasa.jstateexplorer.newTransitionSystem.profiling.ProfiledTransitionLabel;
import gov.nasa.jstateexplorer.transitionSystem.parser.TransitionSystemParser;
import org.antlr.runtime.RecognitionException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author mmuesly
 */
public class ProfiledTransitionSystemTests {
  
  public ProfiledTransitionSystemTests() {
  }

  @Test
  public void errorTransitionKeepErrorFlag() throws RecognitionException {
    TransitionLabel label = new TransitionLabel();
    label.markAsErrorTransitionLabel();
    assertTrue(label.isError());
    TransitionLabel pLabel = new ProfiledTransitionLabel(label);
    assertTrue(pLabel.isError());
  }

  @Test
  public void constructorKeepsConstructorFlag() throws RecognitionException {
    TransitionLabel label = new TransitionLabel();
    label.markAsConstructor();
    assertTrue(label.isConstructor());
    TransitionLabel pLabel = new ProfiledTransitionLabel(label);
    assertTrue(pLabel.isConstructor());
  }
}
