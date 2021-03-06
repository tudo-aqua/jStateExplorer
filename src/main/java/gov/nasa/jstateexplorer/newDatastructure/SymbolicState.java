package gov.nasa.jstateexplorer.newDatastructure;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.TypeContext;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.newTransitionSystem.Transition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * According to our interpretation a state has a set 
 * of constraints on the variables in the state.
 * @author mmuesly
 */
public class SymbolicState extends HashMap<Variable<?>, Expression<Boolean>>{

  private static long counter = 0;
  private boolean isErrorState;
  private String id;
  
  private List<Transition> incomingTransitions, outgoingTransitions;
  
  public synchronized static String getNextStateId() {
    String nextId = "s_" + SymbolicState.counter;
    ++counter;
    return nextId;
  }
  
  public SymbolicState(){
    super();
    this.incomingTransitions = new ArrayList<>();
    this.outgoingTransitions = new ArrayList<>();
    this.id = SymbolicState.getNextStateId();
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
    if(var.getType() instanceof BuiltinTypes.BoolType){
      return new PropositionalCompound(var, LogicalOperator.EQUIV, constant);
    }
    return new NumericBooleanExpression(var, NumericComparator.EQ, constant);
    
  }

  public void addIncomingTransition(Transition transition){
    if(transition != null){
      this.incomingTransitions.add(transition);
    }
  }
  
  public void addOutgoingTransition(Transition transition){
    if(transition != null){
      this.outgoingTransitions.add(transition);
    }
  }

  public boolean hasOutgoingTransitions(){
    return !this.outgoingTransitions.isEmpty();
  }

  public boolean hasIncomingTransitions() {
    return !this.incomingTransitions.isEmpty();
  }
  public Expression<Boolean> toExpression() {
    Expression expr = null;
    for(Expression value: this.values()) {
      expr = ((expr == null)? value: ExpressionUtil.and(expr, value));
    }
    return expr;
  }

  public void markAsErrorState() {
    this.isErrorState = true;
  }

  public boolean isError() {
    return this.isErrorState;
  }
  
  public String getID(){
    return this.id;
  }
  
  public List<Transition> getIncommingTransitions() {
    return new ArrayList<>(this.incomingTransitions);
  }

  public List<Transition> getOutgoingTransitions() {
    return new ArrayList<>(this.outgoingTransitions);
  }
}
