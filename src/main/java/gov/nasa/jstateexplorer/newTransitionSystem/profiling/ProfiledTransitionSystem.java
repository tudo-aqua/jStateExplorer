package gov.nasa.jstateexplorer.newTransitionSystem.profiling;

import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionLabel;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mmuesly
 */
public class ProfiledTransitionSystem extends TransitionSystem{
  
  private HashMap<Integer, Long> iterationTime;
  private HashMap<Integer, Long> reachTime;
  private HashMap<TransitionLabel, Long> labelTime;
  private HashMap<Integer, Long> guardTime;
  private HashMap<TransitionLabel, Long> labelGuardTime;
  private long lastUnrollmentDuration;
  private int currentDepth;
  
  public ProfiledTransitionSystem(){
    super();
    iterationTime = new HashMap<>();
    labelTime = new HashMap<>();
    reachTime = new HashMap<>();
    guardTime = new HashMap<>();
    labelGuardTime = new HashMap<>();
  }
  

  @Override
  public void addTransitionLabel(TransitionLabel newLabel){
    if(newLabel.isExecuted()){
      throw new RuntimeException("Cannot add an already executedLabel");
    }
    ProfiledTransitionLabel converted = new ProfiledTransitionLabel(newLabel);
    super.addTransitionLabel(converted);
  }

  @Override
  public void unrollToDepth(int depth) {
    long startUnrollment = System.currentTimeMillis();
    super.unrollToDepth(depth);
    long stopUnrollment = System.currentTimeMillis();
    lastUnrollmentDuration = stopUnrollment - startUnrollment;
  }

  @Override
  protected void unrollIteration(int currentDepth) {
    this.currentDepth = currentDepth;
    long iterationStart = System.currentTimeMillis();
    super.unrollIteration(currentDepth);
    long iterationEnd = System.currentTimeMillis();
    long iterationDuration = iterationEnd - iterationStart;
    iterationTime.put(currentDepth, iterationDuration);
    calculateGuardTime();
  }
  
  @Override
  protected boolean processTransitionLabelOnState(TransitionLabel label,
          SymbolicState state, int currentDepth) {
    long labelStart = System.currentTimeMillis();
    boolean res = 
            super.processTransitionLabelOnState(label, state, currentDepth);
    long labelEnd = System.currentTimeMillis();
    long lastValue = labelTime.getOrDefault(label, 0L);
    lastValue += labelEnd - labelStart;
    labelTime.put(label, lastValue);
    return res;
  }
  

  @Override
  protected boolean isNewValueInState(SymbolicState resultingState) {
    long startValueTest = System.currentTimeMillis();
    boolean res = super.isNewValueInState(resultingState);
    long endValueTest = System.currentTimeMillis();
    long lastValue = reachTime.getOrDefault(currentDepth, 0L);
    lastValue += endValueTest - startValueTest;
    reachTime.put(currentDepth, lastValue);
    return res;
  }

  @Override
  public int unrollToFixPoint() {
    long unrollmentStart = System.currentTimeMillis();
    int k = super.unrollToFixPoint();
    long unrollmentEnd = System.currentTimeMillis();
    lastUnrollmentDuration = unrollmentEnd - unrollmentStart;
    return k;
  }

  public long getLastUnrollmentDuration(){
    return lastUnrollmentDuration;
  }

  public long getIterationTimeForDepth(int depth) {
    return iterationTime.get(depth);
  }

  public HashMap<Integer, Long> getAllIterationsTime() {
    return new HashMap<>(iterationTime);
  }

  private void calculateGuardTime() {
    List<TransitionLabel> labels = getTransitionLabels();
    long totalDuration = 0L;
    for(TransitionLabel label: labels) {
      if(label instanceof ProfiledTransitionLabel) {
        ProfiledTransitionLabel profiledLabel = (ProfiledTransitionLabel) label;
        long duration = profiledLabel.getDuration();
        profiledLabel.resetDuration();
        totalDuration += duration;
        long labelDuration = labelGuardTime.getOrDefault(label, 0L);
        labelDuration += duration;
        labelGuardTime.put(label, labelDuration);
      }else {
        throw new RuntimeException("Found not allowed Label type.");
      }
    }
    super.setTransitionLabels(labels);
    guardTime.put(currentDepth, totalDuration);
  }
  
  public String completeResultAsString() {
    StringBuilder profile = new StringBuilder();
    profile.append("Total Time for unrolling system: " 
            + lastUnrollmentDuration + "\n");
    profile.append("----------------------------------------------\n");
    profile.append("Time for each iteration step: \n");
    for(Integer key: iterationTime.keySet()){
      profile.append("Depth: " + key 
              + " duration: " + iterationTime.get(key) + " ms\n");
    }
    profile.append("----------------------------------------------\n");
    profile.append("Time for reach calculation in each iteration step: \n");
    for(Integer key: reachTime.keySet()){
      profile.append("Depth: " + key 
              + " duration: " + reachTime.get(key) + " ms\n");
    }
    profile.append("----------------------------------------------\n");
    profile.append("Time for guard calculation in each iteration step: \n");
    for(Integer key: guardTime.keySet()){
      profile.append("Depth: " + key 
              + " duration: " + guardTime.get(key) + " ms\n");
    }
    profile.append("----------------------------------------------\n");
    profile.append("Time spend on transition label \n");
    for(TransitionLabel label: labelTime.keySet()){
      profile.append("Label: " + label.getName()
              + " duration: " + labelTime.get(label) + " ms\n");
    }
    profile.append("----------------------------------------------\n");
    profile.append("Time spend on transition label for guard test: \n");
    for(TransitionLabel label: labelGuardTime.keySet()){
      profile.append("Label: " + label.getName()
              + " duration: " + labelGuardTime.get(label) + " ms\n");
    }
    profile.append("----------------------------------------------\n");
    return profile.toString();
  }
}
