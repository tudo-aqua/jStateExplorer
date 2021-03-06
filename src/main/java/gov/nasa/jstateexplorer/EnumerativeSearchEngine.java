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
import gov.nasa.jstateexplorer.datastructures.region.EnumerativeRegion;
import gov.nasa.jstateexplorer.datastructures.searchImage.EnumerativeImage;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystem;
import gov.nasa.jstateexplorer.util.SearchProfiler;
import gov.nasa.jstateexplorer.util.SearchUtil;
import gov.nasa.jstateexplorer.util.region.EnumerativeRegionUtil;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is an enumerative breadth-first search.
 */
public class EnumerativeSearchEngine {
  private static Logger logger = Logger.getLogger("psyco");
  public static EnumerativeImage enumerativBreadthFirstSearch(
          TransitionSystem transitionSystem,
          ConstraintSolver solver,
          int maxSearchDepth,
          Level level){
    logger.setLevel(level);
    return enumerativBreadthFirstSearch(transitionSystem, solver, maxSearchDepth);
  }

  public static EnumerativeImage enumerativBreadthFirstSearch(
          TransitionSystem transitionSystem,
          ConstraintSolver solver,
          int maxSearchDepth,
          Logger logger){
    EnumerativeSearchEngine.logger = logger;
    return enumerativBreadthFirstSearch(transitionSystem, solver, maxSearchDepth);
  }

  public static EnumerativeImage enumerativBreadthFirstSearch(
          TransitionSystem transitionSystem,
          ConstraintSolver solver,
          int maxSearchDepth) {
    SolverInstance.getInstance().setSolver(solver);
    System.out.println("gov.nasa.jstateexplorer.EnumerativeSearchEngine.enumerativBreadthFirstSearch()");
    System.out.println(transitionSystem.getInitValuation().toString());
    EnumerativeRegion newRegion, reachableRegion
            = new EnumerativeRegion(transitionSystem.getInitValuation());
    EnumerativeRegionUtil regionUtil = new EnumerativeRegionUtil(solver);
    SearchUtil<EnumerativeImage> searchUtil
            = new SearchUtil<>(regionUtil, logger);
    EnumerativeImage currentSearchState
            = new EnumerativeImage(reachableRegion);
    //the last iteration. A fix point is reached. This is the termiantion goal.
    while (!currentSearchState.getPreviousNewStates().isEmpty()) {
      EnumerativeImage newImage = searchUtil.post(currentSearchState,
              transitionSystem);
      StringBuilder a = new StringBuilder();
      try {
        newImage.print(a);
      } catch (IOException ex) {
        Logger.getLogger(EnumerativeSearchEngine.class.getName()).log(Level.SEVERE, null, ex);
      }
      logger.info("new Image: " + a);
      EnumerativeRegion nextReachableStates = newImage.getNewStates();

      SearchProfiler.startDiffProfiler(newImage.getDepth());
      newRegion = regionUtil.difference(nextReachableStates,
              reachableRegion);
      SearchProfiler.stopDiffProfieler(newImage.getDepth());

      a = new StringBuilder();
      try {
        newRegion.print(a);
      } catch (IOException ex) {
        Logger.getLogger(EnumerativeSearchEngine.class.getName()).log(Level.SEVERE, null, ex);
      }
      logger.info("new Region: " + a);
      
      reachableRegion = regionUtil.union(reachableRegion, newRegion);
      newImage.setReachableStates(reachableRegion);
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
    }
    return currentSearchState;
  }

  private static void logState(EnumerativeImage newImage) {
    StringBuilder builder = new StringBuilder();
    try {
      newImage.print(builder);
      logger.fine(builder.toString());
    } catch (IOException ex) {
      Logger.getLogger(SymbolicSearchEngine.class.getName())
              .log(Level.SEVERE, null, ex);
    }
  }
}