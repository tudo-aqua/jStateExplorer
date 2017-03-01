package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jstateexplorer.datastructures.state.State;
import java.util.HashSet;
import java.util.Set;

/**
 * A transition is a path between two states and directed from start to end.
 * Side Informations describing what is done during this transitions and when
 * it is enabled, is stored in the TransitionLabel.
 * If more then one TransitionLabel might describe this Transition, it is 
 * allowed to add more labels.
 * @author mmuesly
 */
public class Transition {
  Set<TransitionLabel> transitionLabels;
  State start, end;

  public Transition(){
    this.transitionLabels = new HashSet<>();
  }
  public Transition(State start, State end){
    this();
    this.start = start;
    this.end = end;
  }
  
  public void addLabel(TransitionLabel label){
    this.transitionLabels.add(label);
  }

  public Set<TransitionLabel> getTransitionLabels(){
    return this.transitionLabels;
  }

  public void setStart(State start){
    this.start = start;
  }

  public void setEnd(State end){
    this.end = end;
  }
}