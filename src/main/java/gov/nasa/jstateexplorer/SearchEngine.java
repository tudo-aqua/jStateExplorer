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
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jstateexplorer.datastructures.searchImage.EnumerativeImage;
import gov.nasa.jstateexplorer.datastructures.searchImage.SymbolicImage;
import gov.nasa.jstateexplorer.transitionSystem.EnumerativeTransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.SymbolicTransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.TransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystem;
import gov.nasa.jstateexplorer.util.HelperMethods;
import gov.nasa.jstateexplorer.util.ResultSaver;
import gov.nasa.jstateexplorer.util.SearchProfiler;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The search engine is the central hook-up point for search algorithms and extensions.
 */
public class SearchEngine {

  private Logger logger;
  private String folderName = "default";
  SearchConfig sconf;

  public SearchEngine(SearchConfig sconf) {
    this.sconf = sconf;
    logger = Logger.getLogger(HelperMethods.getLoggerName());
    updateFolderName(sconf);
  }

  public SearchEngine(SearchConfig sconf, Level level) {
    this.sconf = sconf;
    logger = Logger.getLogger(HelperMethods.getLoggerName());
    logger.setLevel(level);
    updateFolderName(sconf);
  }

  public SearchEngine(SearchConfig sconf, Logger logger) {
    this.sconf = sconf;
    logger = Logger.getLogger(HelperMethods.getLoggerName());
    this.logger =logger;
    updateFolderName(sconf);
  }
  /**
  * This method exchanges results with PSYCOs interface learning
  * algorithm using the a PsycoConfig element.
  * Search results might be written into files, in case PSYCO is configured
  * to do so.
   * @param system The transition system, that should be unrolled.
   * @param solver A Constraint Solver needed to solve Constraints during the search.
   * @return Return the max depth k at which a fix point is reached
  */
  public int executeSearch(TransitionSystem system,
          ConstraintSolver solver) {
    int depthEnumerative = -1, depthSymbolic = -1;
    if (sconf.shouldUseEnumerativeSearch()) {
      SearchProfiler.reset();
      depthEnumerative = executeEnumerativeSearch(system, solver);
    }
    if (sconf.shouldUseSymbolicSearch()) {
      SearchProfiler.reset();
      depthSymbolic = executeSymbolicSearch(system, solver);
    }
    if(depthEnumerative != -1 && depthSymbolic != -1 && depthEnumerative != depthSymbolic){
      String msg = "The enumerative Search and the symbolic Search don't reach the same result!";
      throw new IllegalStateException(msg);
    }else if(depthEnumerative != -1){
      return depthEnumerative;
    }else if(depthSymbolic != -1){
      return depthSymbolic;
    }else{
      String msg = "Cannot determine Search result!";
      throw new RuntimeException(msg);
    }
  }

  private int executeEnumerativeSearch(TransitionSystem system,
          ConstraintSolver solver) {
    logger.info("Start enumerative search");
    System.out.println("gov.nasa.jstateexplorer.SearchEngine.executeEnumerativeSearch()");
    System.out.println("initValuation: " + system.getInitValuation());
    TransitionHelper helper = new EnumerativeTransitionHelper();
    system.setHelper(helper);
    logger.info(system.toString());
    if(sconf.isSaveTransitionSystem()){
      String transitionSystemFile = sconf.getResultFolder() 
              + "/transitionSystem.ts";
      system.writeToFile(transitionSystemFile);
    }
    EnumerativeImage searchResult
            = EnumerativeSearchEngine.enumerativBreadthFirstSearch(
                    system,
                    solver, sconf.getMaxSearchDepth(), logger);
    logger.info("Enumerative search done. Here is the result:");
    StringBuilder searchResultString = new StringBuilder();
    try {
      searchResult.print(searchResultString);
    } catch (IOException ex) {
      Logger.getLogger(SearchEngine.class.getName())
              .log(Level.SEVERE, null, ex);
    }

    logger.fine(searchResultString.toString());
    logger.info("Enumerative Search determined:");
    logger.info("Max search depth k = " + searchResult.getDepth());
    if (searchResult.getDepth() != Integer.MAX_VALUE) {
      logger.info("Set Psyco maxDepth to k.");
    }
    if (sconf.isSaveSearchResult()) {
      String prefix = "enumerative-";
      ResultSaver.writeResultToFolder(searchResult, folderName,
              prefix);
      SearchProfiler.writeRunToFolder(folderName, prefix);
    }
    return searchResult.getDepth();
  }

  private int executeSymbolicSearch(TransitionSystem system,
          ConstraintSolver solver) {
    logger.info("Start symbolic search");
    TransitionHelper helper = new SymbolicTransitionHelper();
    SolverInstance.getInstance().setSolver(solver);
    system.setHelper(helper);
    if(sconf.isSaveTransitionSystem()){
      String transitionSystemFile = sconf.getResultFolder() 
              + "/transitionSystem.ts";
      system.writeToFile(transitionSystemFile);
    }
    logger.fine(system.toString());
    SymbolicImage searchResult
            = SymbolicSearchEngine.symbolicBreadthFirstSearch(
                    system,
                    solver, sconf.getMaxSearchDepth(), logger);
    logger.info("symbolic search terminated for following reason:");
    if (searchResult.getDepth() == Integer.MAX_VALUE) {
      logger.info("Symbolic search hit predefined max"
              + " depth value and was interrupted.");
    } else {
      logger.info("Symbolic search done and terminated by fix point");
    }
    logger.info("However, here is the result:");

    StringBuilder searchResultString = new StringBuilder();
    try {
      searchResult.print(searchResultString);
    } catch (IOException ex) {
      Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
    }
    logger.fine(searchResultString.toString());
    logger.info("Symbolic Search determined:");
    logger.info("Max search depth k = " + searchResult.getDepth());
    if (sconf.isSaveSearchResult()) {
      String prefix = "symbolic-";
      ResultSaver.writeResultToFolder(searchResult,
              system, folderName, prefix);
      SearchProfiler.writeRunToFolder(folderName, prefix);
    }
    return searchResult.getDepth();
  }


  private void updateFolderName(SearchConfig pconf) {
    folderName = pconf.getResultFolder();
    File file = new File(folderName);
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  public void saveProfilerResults() {
    SearchProfiler.writeRunToFolder(folderName);
  }
}