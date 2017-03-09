package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.TypeContext;
import gov.nasa.jstateexplorer.SolverInstance;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.newTransitionSystem.helper.ExpressionQuantifier;
import gov.nasa.jstateexplorer.newTransitionSystem.helper.TransitionSystemHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mmuesly
 */
public class TransitionSystem {
  private List<Variable<?>> stateVariables;
  private List<TransitionLabel> transitionLabels;
  private HashMap<Integer, List<SymbolicState>> states;
  private HashMap<Integer, List<Transition>> transitions;
  
  public TransitionSystem(){
    this.stateVariables = new ArrayList<>();
    this.transitionLabels = new ArrayList<>();
    this.states = new HashMap<>();
    this.transitions = new HashMap<>();
  }

  public List<Variable<?>> getStateVariables(){
    return new ArrayList<>(this.stateVariables);
  }

  public void addVariable(Variable var){
    if(var != null){
      this.stateVariables.add(var);
    }
  }

  public void addVariables(List<Variable<?>> vars){
    if(vars != null){
      this.stateVariables.addAll(vars);
    }
  }

  public void addTransitionLabel(TransitionLabel newLabel){
    if(newLabel != null){
      this.transitionLabels.add(newLabel);
    }
  }
  public List<TransitionLabel> getTransitionLabels() {
    return new ArrayList<>(this.transitionLabels);
  }

  public boolean hasStateVariableWithName(String effectedVariableName) {
    for(Variable var: this.stateVariables){
      if(var.getName().equalsIgnoreCase(effectedVariableName)){
        return true;
      }
    }
    return false;
  }

  public Variable getStateVariableByName(String effectedVariableName) {
    for(Variable var: this.stateVariables){
      if(var.getName().equalsIgnoreCase(effectedVariableName)){
        return var;
      }
    }
    return null;
  }

  public TransitionLabel getTransitionLabelByName(String transitionName) {
    for(TransitionLabel label: this.transitionLabels){
      if(label.getName().equalsIgnoreCase(transitionName)){
        return label;
      }
    }
    return null;
  }

  void setInitState(SymbolicState initState) {
    ArrayList<SymbolicState> initStates = new ArrayList<>();
    initStates.add(initState);
    this.states.put(0, initStates);
  }

  public void initalize() {
    SymbolicState initState = 
            new SymbolicState(this.stateVariables, new TypeContext(true));
    setInitState(initState);
  }
  public SymbolicState getInitState() {
    if(this.states.containsKey(0)){
      List<SymbolicState> initStates = this.states.get(0);
      if(initStates.size() == 1){
        return initStates.get(0);
      }
    }
    return null;
  }

  //Returns last dapth in which a new State has been reached.
  public int unrollToFixPoint() {
    int depth = 0;
    while(!this.states.getOrDefault(depth, new ArrayList<>()).isEmpty()){
      ++depth;
      unrollIteration(depth);
    }
    return --depth;
  }

  void unrollToDepth(int depth) {
    for(int i = 1; i <= depth; i++){
      unrollIteration(i);
    }
  }

  List<SymbolicState> getStatesNewInDepth(int i) {
    return new ArrayList<>(this.states.getOrDefault(i, new ArrayList<>()));
  }

  List<Transition> getTransitionsOfIteration(int i) {
    return this.transitions.getOrDefault(i, new ArrayList<>());
  }

  private void unrollIteration(int i) {
    this.states.put(i, new ArrayList<>());
    this.transitions.put(i, new ArrayList<>());
    if(i == 0){
      throw new RuntimeException(
              "Cannot unroll Iterations in Depth less then 1.");
    }
    System.out.println("gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem.unrollIteration()");
    System.out.println("i: " + i);
    for(SymbolicState state: this.getStatesNewInDepth(i - 1)){
      System.out.println("Init State: " + state.toExpression());
      for(TransitionLabel label: this.transitionLabels){
        if(label.isEnabledOnState(state)){
          //This is an execution of an TransitionLabel and therefore a
          //new Transition. 
          //So it should be added to the Transition System anyhow.
          Transition executedTransition = new Transition();
          executedTransition.setStart(state);
          executedTransition.addLabel(label);
          
          SymbolicState resultingState = label.applyOnState(state,
                  executedTransition.getID());
          executedTransition.setEnd(resultingState);
          if(isNewValueInState(resultingState)){
            this.states.getOrDefault(i, new ArrayList<>()).add(resultingState);
            executedTransition.markReachedNewValue();
          }
          this.transitions.getOrDefault(i, new ArrayList<>())
                  .add(executedTransition);
        }
      }
    }
  }

  private boolean isNewValueInState(SymbolicState resultingState) {
    SolverInstance solver = SolverInstance.getInstance();
    Expression newStateExpression = resultingState.toExpression();
//    newStateExpression = ExpressionQuantifier.existensQuantififaction(
//            newStateExpression, this.stateVariables);
    Expression reachedExpression = 
            TransitionSystemHelper.createReachExpression(this);
//    reachedExpression = ExpressionQuantifier.allQuantification(
//            reachedExpression, this.stateVariables);

    reachedExpression = new Negation(reachedExpression);
    Expression reachedTest = new PropositionalCompound(
            newStateExpression, LogicalOperator.AND, reachedExpression);
    //reachedTest = ExpressionQuantifier.experimentalQuantification(reachedTest, stateVariables);
    System.out.println("reachedExpression: " + reachedTest);
    //Result res = solver.isSatisfiable(reachedTest);
    Valuation resVal = new Valuation();
    Result res = solver.solve(reachedTest, resVal);
    if(res == ConstraintSolver.Result.DONT_KNOW){
      throw new RuntimeException(
              "Cannot decide State reachability!! Abort execution!");
    }
    System.out.println("Result reachTest: " + res);
    System.out.println("Valuation: " +resVal.toString());
    return res == Result.SAT;
    
  }

  public HashMap<Integer, List<SymbolicState>> getAllStates(){
    return new HashMap<>(this.states);
  }
}