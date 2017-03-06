package gov.nasa.jstateexplorer.newTransitionSystem.helper;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Collection;

/**
 * We need to check some properties during TransitionLabel method execution.
 * To increase the TransitionLabel readability, 
 * code for this checks is outsourced to this class.
 * 
 * @author mmuesly
 */
public class TransitionLabelHelper {
  
  public static boolean containsAnyVar(Collection<Variable<?>> toBeFound,
          Collection<Variable <?>> universe) {
    for(Variable variable: toBeFound){
      if(universe.contains(variable)) {
        return true;
      }
    }
    return false;
  }
  
  public static boolean containsAnyVar(Collection<Variable<?>> toBeFound,
          Expression universe) {
    Collection<Variable<?>> varsInExpression = 
            ExpressionUtil.freeVariables(universe);
    return TransitionLabelHelper.containsAnyVar(toBeFound, varsInExpression);
  }
}
