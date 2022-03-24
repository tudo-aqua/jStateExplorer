package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jstateexplorer.newTransitionSystem.helper.RenameUtils;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.SolverInstance;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
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
  private static long counter = 0L;
  
  private String name = "";
  private long id;
  private Set<Variable<?>> variables;
  private List<Variable<?>> parameterVariables;

  private ArrayList<Expression<Boolean>> preConditionConstraints;
  private HashMap<Variable, Expression<Boolean>> effectConstraints;
  private ArrayList<Transition> executingTransition;
  private boolean errorTransititonLabel,isConstructor;

  public synchronized static long getNextID(){
    long toReturn = TransitionLabel.counter;
    ++TransitionLabel.counter;
    return toReturn;
  }
  
  public TransitionLabel(){
    this.variables = new HashSet<>();
    this.parameterVariables = new ArrayList<>();
    this.preConditionConstraints = new ArrayList<>();
    this.effectConstraints = new HashMap<>();
    this.errorTransititonLabel = false;
    this.executingTransition = new ArrayList<>();
    this.id = getNextID();
    this.isConstructor = false;
  }

  public TransitionLabel(String name){
    this();
    this.name = name;
  }

  public TransitionLabel(String name, Collection<Variable<?>> stateVariables){
    this(name);
    this.variables.addAll(stateVariables);
  }

  public void addExecutingTransition(Transition transition){
    if(transition != null){
      this.executingTransition.add(transition);
    }
  }

  public List<Transition> getExecutingTransitions() {
    return new ArrayList<>(this.executingTransition);
  }
  public HashMap<Variable, Expression<Boolean>> getEffectConstraints(){
    return new HashMap<>(this.effectConstraints);
  }

  protected void setEffectConstraints(
          HashMap<Variable, Expression<Boolean>> constaints){
    if(constaints !=  null){
      this.effectConstraints = new HashMap<>(constaints);
    }
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
        if(primeVar.getType() instanceof BuiltinTypes.BoolType){
          returnEffect = new PropositionalCompound(primeVar, LogicalOperator.EQUIV, var);
        } else {
          returnEffect = new NumericBooleanExpression(primeVar, NumericComparator.EQ, var);
        }
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

  public List<Expression<Boolean>> getPreconditionParts(){
    return new ArrayList<>(this.preConditionConstraints);
  }

  protected void setPreconditionParts(List<Expression<Boolean>> preconditions) {
    this.preConditionConstraints = new ArrayList<>(preconditions);
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

  public String getUniqueName(){
    return this.name + "_" + this.id;
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

  protected void setParameterVariables(List<Variable<?>> parameters) {
    if(parameters != null){
      this.parameterVariables = new ArrayList<>(parameters);
    }
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

  public boolean isConstructor() {
    return this.isConstructor;
  }
  
  public void markAsConstructor() {
    this.isConstructor = true;
  }
  public boolean isEnabledOnState(SymbolicState state) {
    Expression expr = ExpressionUtil.and(state.toExpression(),
    this.getPrecondition());
    SolverInstance solver = SolverInstance.getInstance();
    Result res = solver.isSatisfiable(expr);
    if(res == Result.DONT_KNOW){
      throw new RuntimeException(
            "Cannot decide on Precondition, therefore no answer is posisble!");
    }
    return res == Result.SAT;
  }

  public SymbolicState applyOnState(SymbolicState state, long transitionID) {
    SymbolicState resultingState = new SymbolicState();
    for(Variable var: state.keySet()){
      Expression effect = combineEffectAndStateValue(state, var);
      resultingState.put(var, effect);
    }
    resultingState = RenameUtils.rename(resultingState,
            getParameterVariables(), transitionID);
    return resultingState;
  }
  
  private Expression combineEffectAndStateValue(
          SymbolicState state, Variable var){
    Expression effect = getEffectForVariable(var);
    Collection<Variable<?>> variblesInEffect = 
            ExpressionUtil.freeVariables(effect);
    Set<Variable<?>> stateVariables = state.keySet();
    for(Variable stateVar: stateVariables) {
      if(variblesInEffect.contains(stateVar)){
        Expression currentValue = state.get(stateVar);
        effect = ExpressionUtil.and(effect, currentValue);
      }
    }
    return effect;
  }

  public boolean isExecuted() {
    return !this.executingTransition.isEmpty();
  }

  public Collection<Variable<?>> getVariables(){
    return new HashSet<>(this.variables);
  }

  @Override
  public String toString(){
    StringBuilder stringRep = new StringBuilder();
    stringRep.append("TransitionLabel: ");
    stringRep.append(this.name);
    stringRep.append(" unique: ");
    stringRep.append(getUniqueName());
    stringRep.append("\n");
    stringRep.append("Precondition:\n");
    for(Expression preCondition: this.preConditionConstraints){
      stringRep.append(preCondition.toString()).append("\n");
    }
    stringRep.append("EFFECT:\n");
    for(Variable key: this.effectConstraints.keySet()){
      stringRep.append(key.getName()).append(": ");
      stringRep.append(this.effectConstraints.get(key).toString());
      stringRep.append("\n");
    }
    if(isError()){
      stringRep.append("ERROR");
    }
    return stringRep.toString();
  }
}
