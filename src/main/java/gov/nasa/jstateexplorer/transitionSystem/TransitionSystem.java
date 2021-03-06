/*
 * Copyright (C) 2015, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment 
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may obtain a 
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jstateexplorer.transitionSystem;

import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.ExpressionConverterVisitor;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.TransitionEncoding;
import gov.nasa.jstateexplorer.util.HelperMethods;
import gov.nasa.jstateexplorer.util.SearchProfiler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
/**
 * The transition system maintains a set of transitions and some 
 * meta data about the transition system. 
 * For example the amount of stutter transitions, error paths and so on...
 * @author mmuesly
 * @param <T> 
 */
public class TransitionSystem<T extends TransitionHelper> {

  private List<Transition> transitions;
  private Valuation initValuation;
  private T helper;

  private String currentProfilerRun = null;
  private Logger logger = Logger.getLogger(HelperMethods.getLoggerName());

  public TransitionSystem() {
    transitions = new ArrayList<>();
  }

  public TransitionSystem(T helper) {
    transitions = new ArrayList<>();
    initValuation = new Valuation();
    this.helper = helper;
  }

  public TransitionSystem(Valuation initValuation, T helper) {
    this(helper);
    this.initValuation = initValuation;
  }

  public TransitionSystem(
          Valuation initValuation, List<Transition> transitions, T helper) {
    this(helper);
    this.transitions = transitions;
    this.initValuation = initValuation;
  }

  public T getHelper() {
    return helper;
  }

  public void setHelper(T helper) {
    this.helper = helper;
  }

  public void add(Transition t) {
    if (t != null) {
      transitions.add(t);
    }
  }

  public List<Transition> getTransitions() {
    return transitions;
  }

  public List<Transition> getConsideredOkTransitions() {
    ArrayList<Transition> returnList = new ArrayList<>();
    for (Transition t : transitions) {
      if (t.isOk() && !t.isError()) {
        if (t.isStutterTransition()) {
          continue;
        }
        returnList.add(t);
      }
    }
    return returnList;
  }

  public void setTransitions(List<Transition> transitions) {
    if(transitions != null){
      this.transitions = transitions;
    }else{
      String msg = "You are not allowed to set transitions to null.";
      throw new IllegalStateException(msg);
    }
  }

  public Valuation getInitValuation() {
    return initValuation;
  }

  public void setInitValuation(Valuation initValuation) {
    this.initValuation = initValuation;
  }

  public ArrayList<Transition> getConsideredErrorTransitions() {
    ArrayList<Transition> returnList = new ArrayList<>();
    for (Transition t : transitions) {
      if (t.isError()) {
        returnList.add(t);
      }
    }
    return returnList;
  }

  private List<Transition> getStutterTransition() {
    ArrayList<Transition> returnList = new ArrayList<>();
    for (Transition t : transitions) {
      if (t.isStutterTransition()) {
          returnList.add(t);
      }
    }
    return returnList;
  }

  @Override
  public String toString() {
    String transitionSystem = "Transition system:\n";
    transitionSystem += "transitions: " + transitions.size() + "\n";
    List<Transition> okPaths = getConsideredOkTransitions();
    List<Transition> stutterPath = getStutterTransition();
    List<Transition> errors = getConsideredErrorTransitions();
    transitionSystem += "consideredTransitions: "
            + getConsideredOkTransitions().size() + "\n";
    transitionSystem += "stutterTransitions: "
            + stutterPath.size() + "\n";
    transitionSystem += "errorTransitions: "
            + errors.size() + "\n";
    return transitionSystem;
  }

  public String toStringWithID(){
    String trans = "";
    for(Transition t: transitions){
      trans += t.toStringWithId();
    }
    return trans;
  }
  public String completeToString() {
    String transitionSystem = toString();
    List<Transition> okPaths = getConsideredOkTransitions();
    List<Transition> stutterPath = getStutterTransition();
    List<Transition> errors = getConsideredErrorTransitions();
    transitionSystem += "OkPaths:\n";
    transitionSystem += convertPathListToString(okPaths);
    transitionSystem += "stutterPaths:\n";
    transitionSystem += convertPathListToString(stutterPath);
    transitionSystem += "ErrorPaths:\n";
    transitionSystem += convertPathListToString(errors);
    transitionSystem += "Init Variables: ";
    for (Variable var : initValuation.getVariables()) {
      transitionSystem += var.getName() + ",";
    }
    transitionSystem += "\n\n";
    return transitionSystem;
  }

  private String convertPathListToString(List<Transition> transitions) {
    StringBuilder builder = new StringBuilder();
    for (Transition t : transitions) {
      builder.append(t.toString());
      builder.append("\n");
    }
    return builder.toString();
  }

  public SearchIterationImage applyOn(SearchIterationImage alreadyReachedStates) {
    TransitionMonitor.startRuning();
    if (helper == null) {
      throw new IllegalStateException("You must set a TransitionHelper for"
              + " the system, before you can use it.");
    }
    alreadyReachedStates.increaseDepth(1);
    logger.info("enroll Depth: "+ alreadyReachedStates.getDepth());
    SearchProfiler.startTransitionProfiler(alreadyReachedStates.getDepth());
    for (Transition t : transitions) {
      alreadyReachedStates = t.applyOn(alreadyReachedStates, helper);
      if(!TransitionMonitor.isRunning()){
          return alreadyReachedStates;
      }
    }
    SearchProfiler.stopTransitionProfiler(alreadyReachedStates.getDepth());
    return alreadyReachedStates;
  }

  public String getExecutionStatistics() {
    int errorReached = 0;
    int errorNonReached = 0;
    int normalTransitionReached = 0;
    int normalTransitionNotReached = 0;
    for (Transition t : transitions) {
      if (t.isError()) {
        if (t.isReached()) {
          errorReached++;
        } else {
          errorNonReached++;
        }
      }
      if (t.isOk()) {
        if (t.isReached()) {
          normalTransitionReached++;
        } else {
          normalTransitionNotReached++;
        }
      }
    }
    StringBuilder statistic = new StringBuilder();
    statistic.append("This transition system has size: "
            + transitions.size() + "\n");
    statistic.append("There are in total: "
            + Integer.toString(errorReached + errorNonReached)
            + " error paths.\n");
    statistic.append(Integer.toString(errorReached)
            + " error paths have beend reached.\n");
    statistic.append(errorNonReached + " have not been reached.\n");
    statistic.append("Further there are :"
            + Integer.toString(
                    normalTransitionReached + normalTransitionNotReached)
            + " ok paths.\n");
    statistic.append(normalTransitionReached + " could be executed.\n");
    statistic.append(normalTransitionNotReached + " could not be enabled.\n");
    return statistic.toString();
  }

  public void writeToFile(String fileName) {
    try (PrintWriter writer = new PrintWriter(fileName);) {
      ExpressionConverterVisitor visitor = new ExpressionConverterVisitor();
      HashMap<Class, String> data = exctractDataTypes();
      String initState = convert(initValuation, data);
      writer.println(initState);
      for (Transition t : transitions) {
        String transformedTransition = t.convertForFile(data);
        writer.println(transformedTransition);
      }
    } catch (FileNotFoundException ex) {
      logger.severe(ex.toString());

    }
  }

  private HashMap<Class, String> exctractDataTypes() {
    Set<Variable> variables = new HashSet<>();
    HashMap<Class, String> result = new HashMap<>();
    for (Transition t : transitions) {
      if (t.isOk()) {
        variables.addAll(ExpressionUtil.freeVariables(
                t.convertToExpression()));
      }
    }
    for (Variable var : variables) {
      Class clazz = var.getType().getClass();
      result.put(clazz, clazz.getName().replace(";", ""));
    }
    return result;
  }

  private String convert(
          Valuation initValuation, HashMap<Class, String> data) {
    ExpressionConverterVisitor converter = new ExpressionConverterVisitor();
    String result = TransitionEncoding.valuation + ":";
    for (ValuationEntry entry : initValuation) {
      String var = (String) entry.getVariable().accept(converter, data);
      String value = entry.getValue().toString();
      result += TransitionEncoding.valuationEntry + ":" + var
              + ":" + value + ";";
    }
    result += ";";
    return result;
  }
  
  public boolean shouldContinue(SearchIterationImage image){
    return helper.shouldContinue(image);
  }
}