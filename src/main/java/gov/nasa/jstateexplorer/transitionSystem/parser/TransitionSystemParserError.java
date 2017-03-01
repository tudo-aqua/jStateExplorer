package gov.nasa.jstateexplorer.transitionSystem.parser;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemParserError extends Error{

  public TransitionSystemParserError(String line, String reason){
    String errorMsg = "Line: " + line +"\n";
    errorMsg += "Reason: " + reason;
  }
}
