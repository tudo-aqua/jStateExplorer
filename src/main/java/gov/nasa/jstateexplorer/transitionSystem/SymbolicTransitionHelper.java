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
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.SymbolicSearchEngine;
import gov.nasa.jstateexplorer.datastructures.NameMap;
import gov.nasa.jstateexplorer.datastructures.VariableReplacementMap;
import gov.nasa.jstateexplorer.datastructures.region.SymbolicRegion;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.datastructures.searchImage.SymbolicImage;
import gov.nasa.jstateexplorer.datastructures.state.SymbolicEntry;
import gov.nasa.jstateexplorer.datastructures.state.SymbolicState;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.VariableReplacementVisitor;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.VariableRestrictionsVisitor;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.VariableAssignmentVisitor;
import gov.nasa.jstateexplorer.util.HelperMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SymbolicTransitionHelper extends TransitionHelper {

  private final VariableReplacementVisitor replacementVisitor;
  private final VariableRestrictionsVisitor restrictionsVisitor;
  private final VariableAssignmentVisitor assignmentVisitor;

  public SymbolicTransitionHelper() {
    super();
    this.assignmentVisitor = new VariableAssignmentVisitor();
    this.replacementVisitor = new VariableReplacementVisitor();
    this.restrictionsVisitor = new VariableRestrictionsVisitor();
  }

  @Override
  public SearchIterationImage applyOkTransition(SearchIterationImage image, Transition transition) {
    if (image instanceof SymbolicImage) {
      SymbolicRegion newRegion = new SymbolicRegion();
      SymbolicImage currentState = (SymbolicImage) image;
      int depth = currentState.getDepth();
      for (SymbolicState state : 
              currentState.getPreviousNewStates().values()) {
        if (satisfiesGuardCondition(state, transition, depth)) {
          SymbolicState newState = executeTransition(state, transition);
          newRegion.put(HelperMethods.getUniqueStateName(), newState);
        }
      }
      currentState.addNewStates(newRegion);
      return currentState;
    }
    return null;
  }

  private SymbolicState executeTransition(SymbolicState state,
          Transition transition) {
    Expression newValue = null;
    SymbolicState resultingState = new SymbolicState();
    if (!transition.isGuardSymbolicConstant()) {
      newValue = transition.getGuard();
    }
    VariableReplacementMap replacements
            = extractValueReplacements(state);
    for (SymbolicEntry entry : state) {
      SymbolicEntry primeEntry
              = executeTransitionOnEntry(
                      entry, transition, newValue, replacements);
      resultingState.add(primeEntry);
    }
    transition.setReached(true);
    resultingState.copyHistory(state.getHistory());
    resultingState.addToHistory(transition);
    return resultingState;
  }

  private VariableReplacementMap extractValueReplacements(
          SymbolicState state) {
    VariableReplacementMap replacements = new VariableReplacementMap();
    for (SymbolicEntry entry : state) {
      Expression replacement =
            extractValueForReplacement(entry.getVariable(), entry.getValue());
      if (!replacements.containsKey(entry.getVariable())) {
        replacements.put(entry.getVariable(), replacement);
      } else {
        throw new IllegalStateException("IT IS NOT POSSIBLE TO "
                + "REPLACE A VARIABLE WITH TWO VALUES");
      }
    }
    return replacements;
  }

  private SymbolicEntry executeTransitionOnEntry(SymbolicEntry entry,
          Transition transition, Expression prefix,
          VariableReplacementMap replacements) {
    Variable oldVariable = entry.getVariable(),
            primeVariable = createPrimeVariable(oldVariable);
    Expression transitionEffekt = transition.getEffect(oldVariable);
    
    Expression newValue = null;
    if(isTainted(transitionEffekt, prefix, oldVariable)){
      newValue
            = prefix != null
                ? (Expression) prefix.accept(replacementVisitor, replacements)
                : prefix;
    }
//    List<NumericBooleanExpression> oldRestrictionsToKeep = new ArrayList<>();
//    Set<Variable<?>> possibleBound = 
//            ExpressionUtil.freeVariables(entry.getValue());
    newValue = (newValue != null) ? 
                  ExpressionUtil.and(newValue, entry.getValue()): entry.getValue();
//    entry.getValue().accept(restrictionsVisitor, oldRestrictionsToKeep);
//    for (NumericBooleanExpression expr : oldRestrictionsToKeep) {
//      Set<Variable<?>> variables = ExpressionUtil.freeVariables(expr);
//      for (Variable var : variables) {
//        if ((!(var.getName().startsWith("uVarReplacement")))
//                && (!variables.contains(entry.getVariable()))
//                && possibleBound.contains(var)) {
//          newValue = 
//          break;
//        }
//      }
//    }
    if (isStutterEffektForVariable(oldVariable, transitionEffekt)
            || transitionEffekt == null) {
      newValue = createStutterTransition(oldVariable,
              primeVariable, entry.getValue());
    } else if (isConstantAssignment(transitionEffekt)) {
      newValue
              = createConstantAssignment(primeVariable, transitionEffekt);
    } else {
      newValue = createResultValue(oldVariable, primeVariable,
              transitionEffekt, replacements, newValue);
    }
    logger.finest("gov.nasa.jpf.psyco.search.transitionSystem."
            + "SymbolicTransitionHelper.executeTransitionOnEntry()");
    logger.finest("Transition: " + transition.toStringWithId());
    logger.finest("primeVariable: " + primeVariable + " newValue: "
            + newValue + " oldValue: " + entry.getValue());
    return new SymbolicEntry(primeVariable, newValue);
  }

  private boolean isStutterEffektForVariable(Variable oldVariable,
          Expression transitionEffekt) {
    return transitionEffekt instanceof Variable
            && transitionEffekt.equals(oldVariable);
  }

  private Expression createStutterTransition(Variable oldVariable,
          Variable primeVariable, Expression<Boolean> value) {
    NameMap rename = new NameMap();
    rename.mapNames(oldVariable.getName(), primeVariable.getName());
    return ExpressionUtil.renameVars(value, rename);
  }

  private boolean isConstantAssignment(Expression transitionEffekt) {
    return transitionEffekt instanceof Constant;
  }

  private Expression createConstantAssignment(Variable primeVariable,
          Expression transitionEffekt) {
    return NumericBooleanExpression.create(primeVariable,
            NumericComparator.EQ, transitionEffekt);
  }

  private Expression createResultValue(Variable oldVariable,
          Variable primeVariable,
          Expression transitionEffekt,
          VariableReplacementMap replacements,
          Expression prefix) {
    transitionEffekt = (Expression)
            transitionEffekt.accept(replacementVisitor, replacements);
    Expression newValuePart = NumericBooleanExpression.create(primeVariable,
            NumericComparator.EQ, transitionEffekt);
    return simplifyNewValue(appendNewValue(prefix, newValuePart), primeVariable);
  }

  private Expression extractValueForReplacement(Variable oldVariable,
          Expression oldValue) {
    HashMap<Variable, List<Expression>> data = new HashMap<>();
    data.put(oldVariable, new ArrayList<Expression>());
    oldValue.accept(assignmentVisitor, data);
    List list = data.get(oldVariable);
    if (list.size() > 1) {
      throw new IllegalStateException(
              "Cannot handle undefined value assignment");
    } else if (list.size() == 1) {
      return (Expression) list.get(0);
    }
    return oldVariable;
  }

  private Expression appendNewValue(Expression newValue, Expression toAppend) {
    newValue = (newValue == null ? toAppend
            : ExpressionUtil.and(newValue, toAppend));
    return newValue;
  }

  private Variable createPrimeVariable(Variable var) {
    String newName = var.getName() + "'";
    return Variable.create(var.getType(), newName);
  }

  private boolean isTainted(Expression transitionEffekt, Expression guard, Variable oldVariable) {
    if(guard == null || transitionEffekt == null){
      return false;
    }
    Set<Variable<?>> effectVariables = ExpressionUtil.freeVariables(transitionEffekt);
    effectVariables.remove(oldVariable);
    if(effectVariables.isEmpty()){
      return false;
    }else{
      effectVariables.add(oldVariable);
    }
    Set<Variable<?>> guardVariables = ExpressionUtil.freeVariables(guard);
    for(Variable effectVar: effectVariables){
      if(guardVariables.contains(effectVar)){
        return true;
      }
    }
    return false;
  }

  private Expression simplifyNewValue(Expression appendNewValue , Variable prime) {
    Valuation res = new Valuation();
    Result ret = solver.solve(appendNewValue, res);
    if(simplifiableValue(appendNewValue, prime.getName()) && ret == Result.SAT){
      Object newValue = res.getValue(prime);
      Expression constant = new Constant(prime.getType(), newValue);
      return NumericBooleanExpression.create(prime, NumericComparator.EQ, constant);
    }
    return appendNewValue;
  }

  //FiXME: Is this condition enough?
  private boolean simplifiableValue(Expression newValue, String primeName){
    Set<Variable<?>> variablesInExpression = ExpressionUtil.freeVariables(newValue);
    //System.out.println("simplifiableValue");
    //System.out.println("newValue" + newValue);
    for(Variable candidate: variablesInExpression){
      //System.out.println("primeName: " + primeName + " candidate: " + candidate.getName());
      if(!primeName.startsWith(candidate.getName())){
    //    System.out.println("false");
        return false;
      }
    }
  //  System.out.println("true");
    return true;
  }
}