package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.newTransitionSystem.helper.TransitionLabelHelper;
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
  private List<Variable<?>> parameterVariables;

  private ArrayList<Expression<Boolean>> preConditionConstraints;
  private HashMap<Variable, Expression<Boolean>> effectConstraints;

  private boolean errorTransititonLabel;

  public TransitionLabel(){
    this.variables = new HashSet<>();
    this.parameterVariables = new ArrayList<>();
    this.preConditionConstraints = new ArrayList<>();
    this.effectConstraints = new HashMap<>();
    this.errorTransititonLabel = false;
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
      Expression returnEffect = null;
      if(effectConstraints.containsKey(var)){
        returnEffect = this.effectConstraints.get(var);
        //For the precondition must all parts containing
        //any effect variable be included. 
        Expression relevantPrecondition = 
                createRelevantPrecondition(
                        ExpressionUtil.freeVariables(returnEffect),
                        this.preConditionConstraints.size());
        if(relevantPrecondition != null) {
          returnEffect = ExpressionUtil.and(relevantPrecondition, returnEffect);
        }
      }else {
        //From a logical perspective, this doesn't change var value during the
        //transition.
        Variable primeVar = new Variable(var.getType(), var.getName() + "'");
        returnEffect = new NumericBooleanExpression(
                            primeVar, NumericComparator.EQ, var);
      }
      return returnEffect;
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

  public List<Variable<?>> getParameterVariables() {
    return new ArrayList(this.parameterVariables);
  }

  private Expression createRelevantPrecondition(
          Collection<Variable<?>> variables, int maxIndex) {
    Expression expr = null;
    for(int i = 0; i < maxIndex; i++){
      Expression preconditionPart = this.preConditionConstraints.get(i);
      if(TransitionLabelHelper.containsAnyVar(variables, preconditionPart)){
        expr = 
              (expr != null)? 
                  ExpressionUtil.and(expr, preconditionPart)
                  : preconditionPart;
        expr = catchUpPotentiallyMissedParts(expr, variables, i);

      }
    }
    return expr;
  }
  
  private boolean updateVariables(Collection<Variable<?>> variables,
          Collection<Variable<?>> additionalVariables) {
    for(Variable candidate: additionalVariables){
      if(!variables.contains(candidate)){
        variables.add(candidate);
      }
    }
    return variables.containsAll(additionalVariables);
  }

  private Expression catchUpPotentiallyMissedParts(Expression expr,
          Collection<Variable<?>> variables, int currentPosition) {
    Collection<Variable<?>> additionalVariables = 
                ExpressionUtil.freeVariables(expr);
    if(!variables.containsAll(additionalVariables)) {
      Expression otherPrecondition = 
              createRelevantPrecondition(additionalVariables, currentPosition);
      if(otherPrecondition != null){
        expr = ExpressionUtil.and(expr, otherPrecondition);
      }
      updateVariables(variables, additionalVariables);
    }
    return expr;
  }

  public boolean isError() {
    return this.errorTransititonLabel;
  }

  public void markAsErrorTransitionLabel() {
    this.errorTransititonLabel = true;
  }
}
