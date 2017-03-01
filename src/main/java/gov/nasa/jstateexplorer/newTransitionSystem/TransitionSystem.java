package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jpf.constraints.api.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mmuesly
 */
public class TransitionSystem {
  private List<Variable<?>> stateVariables;
  private List<TransitionLabel> transitionLabels;
  public TransitionSystem(){
    this.stateVariables = new ArrayList<>();
    this.transitionLabels = new ArrayList<>();
  }

  public List<Variable<?>> getStateVariables(){
    return this.stateVariables;
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
    return this.transitionLabels;
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

  public TransitionLabel getTransitionLabelByName(String h2) {
    for(TransitionLabel label: this.transitionLabels){
      if(label.getName().equalsIgnoreCase(h2)){
        return label;
      }
    }
    return null;
  }

}