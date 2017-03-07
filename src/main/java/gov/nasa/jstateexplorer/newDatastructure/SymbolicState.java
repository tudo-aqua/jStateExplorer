package gov.nasa.jstateexplorer.newDatastructure;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.TypeContext;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Collection;
import java.util.HashMap;

/**
 * According to our interpretation a state has a set 
 * of constraints on the variables in the state.
 * @author mmuesly
 */
public class SymbolicState extends HashMap<Variable, Expression<Boolean>>{
  
  public SymbolicState(){
    super();
  }

  public SymbolicState(Collection<Variable<?>> stateVariables,
          TypeContext types) {
    this();
    setToStateVariables(stateVariables, types);
  }

  public void setToStateVariables(Collection<Variable<?>> stateVariables,
          TypeContext types) {
    for(Variable stateVariable: stateVariables) {
      this.put(stateVariable, getInitValue(stateVariable, types));
    }
  }

  private Expression getInitValue(Variable var, TypeContext types) {
    Constant constant = 
            new Constant(var.getType(), var.getType().getDefaultValue());
    return new NumericBooleanExpression(var, NumericComparator.EQ, constant);
    
  }

  public Expression<Boolean> toExpression() {
    Expression expr = null;
    for(Expression value: this.values()) {
      expr = ((expr == null)? value: ExpressionUtil.and(expr, value));
    }
    return expr;
  }
}
