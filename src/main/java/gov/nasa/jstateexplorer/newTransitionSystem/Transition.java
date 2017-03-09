package gov.nasa.jstateexplorer.newTransitionSystem;

import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.newTransitionSystem.helper.TransitionIDGenerator;
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
  private Set<TransitionLabel> transitionLabels;
  private long id;
  private SymbolicState start, end;
  
  private boolean reachedNewValue;
  private boolean reachedError;

  public Transition(){
    this.id = TransitionIDGenerator.getNext();
    this.transitionLabels = new HashSet<>();
  }
  public Transition(SymbolicState start, SymbolicState end){
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

  public void setStart(SymbolicState start){
    this.start = start;
  }

  public void setEnd(SymbolicState end){
    this.end = end;
  }

  public SymbolicState getReachedState() {
    return this.end;
  }

  public SymbolicState getStartState() {
    return this.start;
  }
  
  public long getID(){
    return this.id;
  }

  public void markReachedNewValue() {
    this.reachedNewValue = true;
  }

  public void markReachedErrorState() {
    this.reachedError = true;
  }

  public boolean hasReachedNewValue() {
    return this.reachedNewValue;
  }

  boolean hasReachedErrorState() {
    return this.reachedError;
  }
}