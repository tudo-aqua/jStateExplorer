package gov.nasa.jstateexplorer.newTransitionSystem.helper;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Quantifier;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author mmuesly
 */
public class ExpressionQuantifier {
  
  public static Expression<Boolean> existensQuantification(Expression expr,
          Collection<Variable<?>> stateVariables) {    
    List<Variable<?>> toBind = collectNonStateVariable(expr, stateVariables);
    if(toBind.isEmpty()){
      return expr;
    }    
    return new QuantifierExpression(Quantifier.EXISTS,
              collectNonStateVariable(expr, stateVariables), expr);
  }

  private static ArrayList<Variable<?>> collectNonStateVariable(
          Expression expr, Collection<Variable<?>> stateVariables){
    ArrayList<Variable<?>> toBind = new ArrayList<>();
    Collection<Variable<?>> variablesInExpression = 
            ExpressionUtil.freeVariables(expr);
    for(Variable var: variablesInExpression){
      if(!stateVariables.contains(var)){
        toBind.add(var);
      }
    }
    return toBind;
  }
}
