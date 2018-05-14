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

import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.SolverInstance;
import gov.nasa.jstateexplorer.datastructures.region.Region;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.datastructures.state.State;
import gov.nasa.jstateexplorer.util.HelperMethods;
import gov.nasa.jstateexplorer.util.SearchProfiler;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TransitionHelper executes a transition on a state.
 * Different helpers are needed for symbolic and enumerative search algorithms.
 * This class might be extended for other search algorithms in future.
 */
public abstract class TransitionHelper {

  protected SolverInstance solver = SolverInstance.getInstance();
  protected static final Logger logger
          = Logger.getLogger(HelperMethods.getLoggerName());

  public SearchIterationImage applyTransition(SearchIterationImage image,
          Transition transition){
      if(transition.isOk()){
          return applyOkTransition(image, transition);
      }else if (transition.isError()) {
      return applyError(image, transition);
    } else {
      String msg = "This transition is neither ok nor erroneous. Cannot apply!";
      throw new IllegalStateException(msg);
    }
  }
  public abstract SearchIterationImage applyOkTransition(SearchIterationImage image,
          Transition transition);

  public SearchIterationImage applyError(SearchIterationImage searchStatus,
          Transition transition) {
    int depth = searchStatus.getDepth();
    String error = transition.getErrorMessage();
    Region<?, State<?>> reachableStates = searchStatus.getReachableStates();
    for (State state : reachableStates.values()) {
      if (satisfiesGuardCondition(state, transition, depth)
              && !transition.isReached()) {
        transition.setReached(true);
        searchStatus.addErrorInCurrentDepth(error);
      }
    }
    return searchStatus;
  }

  protected boolean satisfiesGuardCondition(State state,
          Transition transition, int depth) throws IllegalStateException {
    Expression guardTest = state.toExpression();
    if(guardTest != null){
      guardTest = ExpressionUtil.and(guardTest, transition.getGuard());
    }else{
      guardTest = transition.getGuard();
    }
    SearchProfiler.startGuardProfiler(depth);
    Result res = solver.isSatisfiable(guardTest);
    SearchProfiler.stopGuardProfiler(depth);
    if (null != res) {
      switch (res) {
        case SAT:
          return true;
        case DONT_KNOW:
          throw new IllegalStateException(
                  "Cannot handle DONT_KNOW in the guard test.");
        default:
          return false;
      }
    }
    throw new IllegalStateException(
            "The solver result is not allowed to be null.");
  }
  
  public boolean shouldContinue(SearchIterationImage image) {
    return true;
  }
}