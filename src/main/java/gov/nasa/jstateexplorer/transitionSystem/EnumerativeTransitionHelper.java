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
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.datastructures.region.EnumerativeRegion;
import gov.nasa.jstateexplorer.datastructures.searchImage.EnumerativeImage;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.datastructures.state.EnumerativeState;
import gov.nasa.jstateexplorer.util.HelperMethods;
import java.util.HashSet;
import java.util.Set;

public class EnumerativeTransitionHelper extends TransitionHelper {

  @Override
  public SearchIterationImage applyOkTransition(SearchIterationImage image, Transition transition) {
    if (image instanceof EnumerativeImage) {
      EnumerativeImage currentSearchStatus = (EnumerativeImage) image;
      int depth = currentSearchStatus.getDepth();
      EnumerativeRegion newRegion = new EnumerativeRegion();
      for (EnumerativeState state
              : currentSearchStatus.getPreviousNewStates().values()) {
        System.out.println("gov.nasa.jstateexplorer.transitionSystem.EnumerativeTransitionHelper.applyOkTransition()");
        System.out.println("state: " + state.getHistoryAsString());
        if (satisfiesGuardCondition(state, transition, depth)) {
          transition.setReached(true);
          EnumerativeState newState = executeTransition(transition, state);
          newRegion.put(HelperMethods.getUniqueStateName(), newState);
        }
      }
      currentSearchStatus.addNewStates(newRegion);
      return currentSearchStatus;
    }
    return null;
  }

  private EnumerativeState executeTransition(Transition transition,
          EnumerativeState state) {
    Expression resultingExpression = state.toExpression();
    Set primeVariablesForStateVars = createFilterVariabel(resultingExpression);
    Expression transitionEffects
            = transition.convertToExpression();
    resultingExpression
            = ExpressionUtil.and(resultingExpression, transitionEffects);
    logger.info("gov.nasa.jpf.psyco.search.transitionSystem."
            + "EnumerativeTransitionHelper.executeTransition()");
    logger.info(resultingExpression.toString());

    System.out.println("gov.nasa.jstateexplorer.transitionSystem.EnumerativeTransitionHelper.executeTransition()");
    System.out.println(primeVariablesForStateVars);
    Valuation result = new Valuation();
    Result res = solver.solve(resultingExpression, result);
    logger.finest("Valuation: " + result.toString());
    Valuation filtered = new Valuation();
    for (ValuationEntry entry : result) {
      if (primeVariablesForStateVars.contains(entry.getVariable())) {
      System.out.println("gov.nasa.jstateexplorer.transitionSystem.EnumerativeTransitionHelper.executeTransition()");
      System.out.println("Var: " + entry.getVariable() + " Value: " + entry.getValue());
        filtered.addEntry(entry);
      }
    }
    if (res == Result.SAT) {
      return new EnumerativeState(filtered);
    } else {
      throw new IllegalStateException("Solver could not SAT state result.");
    }
  }

  private Set createFilterVariabel(Expression resultingExpression) {
    Set<Variable<?>> oldVariables = ExpressionUtil.freeVariables(resultingExpression);
    Set<Variable<?>> filterVariable = new HashSet();
    for (Variable var: oldVariables){
      String currentName = var.getName();
      String newName = currentName + "'";
      Variable filter = new Variable(var.getType(), newName);
      filterVariable.add(filter);
    }
    return filterVariable;
  }
}