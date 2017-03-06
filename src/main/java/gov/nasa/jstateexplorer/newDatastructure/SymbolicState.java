package gov.nasa.jstateexplorer.newDatastructure;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
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
    if(var.getType() instanceof BuiltinTypes){
      if(var.getType() == BuiltinTypes.BOOL){
        return ExpressionUtil.FALSE;
      }else {
        return new Constant(var.getType(), 0);
      }
    } else{
      throw new RuntimeException("Don't know how to initalize this type");
    }
  }
}
