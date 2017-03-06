package gov.nasa.jstateexplorer.transitionSystem.parser;

import java.util.Arrays;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemParserHelper {
  
  /**An effect line is supposed to be of the Form:
   * var: jConstraintExpression
   * So this Method extracts the String Part,
   * that encodes the jConstraintExpression.
   * @param line a String encoding an effect of an TransitionLabel
   * @return jConstraintExpression as String.
   */
  public static String extractEffect(String line){
    String[] lineParts = line.split(":");
    lineParts = Arrays.copyOfRange(lineParts, 1, lineParts.length);
    return String.join(":", lineParts);
  }

  /**An effect line is supposed to be of the Form:
   * var: jConstraintExpression
   * @param line a String encoding an effect of an TransitionLabel
   * @return name of the variable.
   */
  public static String extractEffectedVariableName(String line){
      String[] lineParts = line.split(":");
      return lineParts[0].replace(" ", "");
  }
}
