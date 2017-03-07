package gov.nasa.jstateexplorer.newTransitionSystem.helper;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Quantifier;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author mmuesly
 */
public class ExpressionQuantifier {
  
  public static Expression<Boolean> existensQuantififaction(Expression expr,
          Collection<Variable<?>> stateVariables) {
      return new QuantifierExpression(Quantifier.EXISTS,
              new ArrayList<>(stateVariables), expr);
  }

  public static Expression<Boolean> allQuantification(Expression expr,
          Collection<Variable<?>> stateVariables) {
    return new QuantifierExpression(Quantifier.FORALL,
            new ArrayList<>(stateVariables), expr);
  }
}
