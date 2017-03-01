package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * 
 * @author mmuesly(Malte Mues)
 */
public class TransitionLabel {
  private String name = "";
  private Set<Variable<?>> variables;
  private Set<Variable<?>> parameterVariables;
  
  private ArrayList<Expression<Boolean>> preConditionConstraints;
  private HashMap<Variable, Expression<Boolean>> effectConstraints;
  
  public TransitionLabel(){
    this.variables = new HashSet<>();
    this.parameterVariables = new HashSet<>();
    this.preConditionConstraints = new ArrayList<>();
    this.effectConstraints = new HashMap<>();
  }

  public TransitionLabel(String name){
    this();
    this.name = name;
  }

  public TransitionLabel(String name, Collection<Variable<?>> stateVariables){
    this(name);
    this.variables.addAll(stateVariables);
  }

  public boolean correctInstantiated(){
    return false;
  }

  public Expression<Boolean> getEffectForVariable(Variable var){
    if(this.variables.contains(var) && !this.parameterVariables.contains(var)) {
      Variable primeVar = new Variable(var.getType(), var.getName() + "'");
      Expression<Boolean> nonEffectExpr = 
              new NumericBooleanExpression(primeVar, NumericComparator.EQ, var);
      return this.effectConstraints.getOrDefault(var, nonEffectExpr);
    }
    return null;
  }

  public Expression<Boolean> getPrecondition(){
    return ExpressionUtil.and(this.preConditionConstraints);
  }

  public Expression<Boolean> getEffect(){
    Expression precondition = getPrecondition();
    Expression completeEffect = 
            ExpressionUtil.and(this.effectConstraints.values());
    return ExpressionUtil.and(precondition, completeEffect);
  }

  public void addVariable(Variable var){
    this.variables.add(var);
  }

  public void addParameterVariable(Variable var){
    this.parameterVariables.add(var);
    addVariable(var);
  }

  public void addPrecondition(Expression preconditionPart){
    this.preConditionConstraints.add(preconditionPart);
  }
  
  //I am not sure yet about this. Maybe we should keep a HashMap
  //with the Variable as key for this.
  public void addEffect(Variable var, Expression effect){
    this.effectConstraints.put(var, effect);
  }
  
  public String getName(){
    return this.name;
  }

  public void addEffect(String effectedVariableName, Expression<Boolean> effect) {
    Variable effectedVar = findVariable(effectedVariableName);
    addEffect(effectedVar, effect);
  }

  private Variable findVariable(String name){
    for(Variable var: this.variables){
      if(var.getName().equalsIgnoreCase(name)){
        return var;
      }
    }
    return null;
  }
}
