package gov.nasa.jstateexplorer.newTransitionSystem.profiling;

import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionLabel;
import java.util.Collection;

/**
 *
 * @author mmuesly
 */
public class ProfiledTransitionLabel extends TransitionLabel{

  private long duration;
  
  public ProfiledTransitionLabel(){
    super();
  }
  
  public ProfiledTransitionLabel(String name){
    super(name);
  }

  public ProfiledTransitionLabel(
          String name, Collection<Variable<?>> stateVariables){
    super(name, stateVariables);
  }

  public ProfiledTransitionLabel(TransitionLabel label){
    super(label.getName(), label.getVariables());
    super.setParameterVariables(label.getParameterVariables());
    super.setEffectConstraints(label.getEffectConstraints());
    super.setPreconditionParts(label.getPreconditionParts());
    if(label.isError()){
      super.markAsErrorTransitionLabel();
    }
    if(label.isConstructor()){
      super.markAsConstructor();
    }
  }

  @Override
  public boolean isEnabledOnState(SymbolicState state) {
    long enabledTest = System.currentTimeMillis();
    boolean res = super.isEnabledOnState(state);
    long endTest = System.currentTimeMillis();
    this.duration += endTest - enabledTest;
    return res;
  }

  public long getDuration() {
    return this.duration;
  }

  public void resetDuration() {
    this.duration = 0L;
  }
}
