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
package gov.nasa.jstateexplorer;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jstateexplorer.datastructures.region.Region;
import gov.nasa.jstateexplorer.datastructures.region.SymbolicRegion;
import gov.nasa.jstateexplorer.datastructures.searchImage.SymbolicImage;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystem;
import gov.nasa.jstateexplorer.util.SearchProfiler;
import gov.nasa.jstateexplorer.util.SearchUtil;
import gov.nasa.jstateexplorer.util.region.SymbolicRegionUtil;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* This class implements a Symbolic Breadth-First search The Basis for the
* Algorithmen is taken from Alur, R.:2015. 3.4.2 Symbolic Breadth-First Search.
* In: Alur, R. Principles of Cyber-Physical Systems. Cambridge, London: The MIT
* Press I cite: Input: A transition system T given by regions Init for the
* initial states and Trans for transitions, and a region q for the property.
* Output: If q is reachable in T, return true, else false
*
* reg Reach := Init; reg New := Init; while isEmtpy(new) = 0 do{ if
* isEmpty(Conj(New, q)) = 0 then return true; New :=
* Diff(Post(New,Trans),Reach); Reach := Disj(Reach, New); } return false;
*/
public class SymbolicSearchEngine {

  static String loggerName = "psyco";
  private static Logger logger = Logger.getLogger(loggerName);
  public static SymbolicImage symbolicBreadthFirstSearch(
          TransitionSystem transitionSystem,
          ConstraintSolver solver,
          int maxSearchDepth, Level logLevel){
    logger.setLevel(logLevel);
    return symbolicBreadthFirstSearch(transitionSystem, solver, maxSearchDepth);
  }
  public static SymbolicImage symbolicBreadthFirstSearch(
          TransitionSystem transitionSystem,
          ConstraintSolver solver,
          int maxSearchDepth, Logger logger){
    SymbolicSearchEngine.logger = logger;
    return symbolicBreadthFirstSearch(transitionSystem, solver, maxSearchDepth);
  }
  public static SymbolicImage symbolicBreadthFirstSearch(
          TransitionSystem transitionSystem,
          ConstraintSolver solver,
          int maxSearchDepth) {
    SolverInstance.getInstance().setSolver(solver);
    SymbolicRegion newRegion, reachableRegion
            = new SymbolicRegion(transitionSystem.getInitValuation());
//It would be an interesting research question, to figure this out
//    boolean isLimitedTransitionSystem = transitionSystem.isLimited();
//    logLimit(isLimitedTransitionSystem);
    SymbolicRegionUtil regionUtil = new SymbolicRegionUtil(solver);
    SearchUtil<SymbolicImage> searchUtil
            = new SearchUtil<>(regionUtil, logger);
    //We start to count interation based on 1. 0 is skipped.
    SymbolicImage currentSearchState
            = new SymbolicImage(reachableRegion);
    while (!currentSearchState.getPreviousNewStates().isEmpty()) {
      SymbolicImage newImage = searchUtil.post(currentSearchState,
              transitionSystem);
      SymbolicRegion nextReachableStates = newImage.getNewStates();
      logger.finer("gov.nasa.jstateexplorer.SymbolicSearchEngine.symbolicBreadthFirstSearch()");
      logger.finer("nextReachableStates: \n");
      logRegion(nextReachableStates);
      SearchProfiler.startDiffProfiler(newImage.getDepth());
      newRegion = regionUtil.difference(nextReachableStates,
              reachableRegion, solver);
      SearchProfiler.stopDiffProfieler(newImage.getDepth());
      logger.finer("newRegion:\n");
      logRegion(newRegion);
      reachableRegion = regionUtil.union(reachableRegion, newRegion);

      newImage.setReachableStates(reachableRegion);
      newImage.setPreviousNewStates(newRegion);
      newImage.setNewStates(null);
      currentSearchState = newImage;

      logState(currentSearchState);
      if (maxSearchDepth != Integer.MIN_VALUE
              && currentSearchState.getDepth() == maxSearchDepth) {
        currentSearchState.setDepth(Integer.MAX_VALUE);
        break;
      }
      if(!transitionSystem.shouldContinue(currentSearchState)){
        break;
      }
    }
    return currentSearchState;
  }

  private static void logState(SymbolicImage newImage) {
    Logger logger = Logger.getLogger("psyco");
    StringBuilder builder = new StringBuilder();
    try {
      builder.append("gov.nasa.jstateexplorer.SymbolicSearchEngine.logState()\n");
      newImage.print(builder);
      logger.fine(builder.toString());
    } catch (IOException ex) {
      Logger.getLogger(SymbolicSearchEngine.class.getName())
              .log(Level.SEVERE, null, ex);
    }
  }

  private static void logLimit(boolean limitedTransitionSystem) {
    Logger logger = Logger.getLogger("psyco");
    if (!limitedTransitionSystem) {
      logger.info("");
      logger.info("The Transition system is not finite.");
      logger.info("It is very likely, that the search does not terminate.");
      logger.info("");
    } else {
      logger.info("");
      logger.info("The Transition system seems to be finite.");
      logger.info("The search should terminate.");
      logger.info("");
    }
  }
  
  private static void logRegion(Region region){
    try {
      StringBuilder sb = new StringBuilder();
      region.print(sb);
      logger.finer(sb.toString());
    } catch (IOException ex) {
      Logger.getLogger(SymbolicSearchEngine.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public static String getSearchLoggerName() {
    return loggerName;
  }
}