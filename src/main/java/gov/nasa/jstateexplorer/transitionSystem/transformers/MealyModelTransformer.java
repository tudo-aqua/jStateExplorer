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
package gov.nasa.jstateexplorer.transitionSystem.transformers;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.psyco.alphabet.SymbolicMethodSymbol;
import gov.nasa.jpf.psyco.learnlib.SymbolicQueryOutput;
import gov.nasa.jpf.psyco.search.transitionSystem.SynchronisedTransitionHelper;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.constraints.Path;
import gov.nasa.jpf.jdart.constraints.PathResult;
import gov.nasa.jpf.jdart.constraints.PostCondition;
import gov.nasa.jpf.psyco.search.transitionSystem.Transition;
import gov.nasa.jpf.psyco.search.transitionSystem.TransitionSystem;
import java.util.Collection;
import javax.swing.SwingWorker;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.compact.CompactMealyTransition;

public class MealyModelTransformer {

  private TransitionSystem<SynchronisedTransitionHelper> transitionSystem;
  public MealyModelTransformer(){
    transitionSystem = initalizeTransitionSystem();
  }

  public TransitionSystem transformModel(MealyMachine<
                  Object, SymbolicMethodSymbol, ?, SymbolicQueryOutput> a,
                  Collection<? extends SymbolicMethodSymbol> clctn){
    //prepares the internal transitionSystem for the transformation
    transitionSystem = initalizeTransitionSystem();
    //Convert each Mealy transition into a Transition of psycos transition system.
    for(Object state: a.getStates()){
      for(SymbolicMethodSymbol symbol: clctn){
        for(Object transition: a.getTransitions(state, symbol)){
          if(transition instanceof CompactMealyTransition
                  && state instanceof Integer){
          CompactMealyTransition t = (CompactMealyTransition) transition;
          Transition convTransition = createTransition(t, (Integer) state, symbol);
          transitionSystem.add(convTransition);
          }else{
            throw new RuntimeException("Don't know this kind of mealy automata");
          }
        }
      }
    }
    return transitionSystem;
  }

  private Transition createTransition(CompactMealyTransition t, Integer state, SymbolicMethodSymbol symbol) {
    SymbolicQueryOutput output = (SymbolicQueryOutput) t.getOutput();
    if(output == SymbolicQueryOutput.ERROR){
      return createErrorTransition(t, state, symbol);
    }else if(output == SymbolicQueryOutput.OK){
      return createOKTransition(t, state, symbol);
    }else{
      throw new RuntimeException("Cannot convert this kind of transition.");
    }
  }

  private TransitionSystem initalizeTransitionSystem() {
    SynchronisedTransitionHelper helper = new SynchronisedTransitionHelper();
    transitionSystem = new TransitionSystem<>(helper);
    Valuation initValuation = new Valuation();
    Variable state = new Variable(BuiltinTypes.SINT32, "this.state");
    initValuation.addEntry(new ValuationEntry<>(state, 0));
    transitionSystem.setInitValuation(initValuation);
    return transitionSystem;
  }

  private Variable createStateVariable(){
    return new Variable(BuiltinTypes.SINT32, "this.state");
  }

  private Constant createConstant(Integer value){
    return new Constant(BuiltinTypes.SINT32, value);
  }

  private Transition createErrorTransition(CompactMealyTransition t, Integer state, SymbolicMethodSymbol symbol) {
    Expression guard = createGuard(symbol, state);
    Integer successor = t.getSuccId();
    String error = "State: " + state + " -> ";
    error += symbol.toString();
    error += " -> " + "State': " + successor;
    Path p = new Path(guard, new PathResult.ErrorResult(null, error, null));
    Transition convTrans = new Transition(p);
    convTrans.setId(symbol.getId());
    return convTrans;
  }

  private Transition createOKTransition(CompactMealyTransition t, Integer state, SymbolicMethodSymbol symbol) {
    Expression guard = createGuard(symbol, state);
    Integer successor = t.getSuccId();
    PostCondition effects = new PostCondition();
    effects.addCondition(createStateVariable(), createConstant(successor));
    Path p = new Path(guard, new PathResult.OkResult(null, effects));
    Transition convTrans = new Transition(p);
    convTrans.setId(symbol.getId());
    return convTrans;
  }

  private Expression createGuard(SymbolicMethodSymbol symbol, Integer start){
    Variable startState = createStateVariable();
    Constant stateValue = createConstant(start);
    Expression guard = 
            new NumericBooleanExpression(startState,
                    NumericComparator.EQ, stateValue);
    if(!ExpressionUtil.isTrue(symbol.getPrecondition())){
      guard = ExpressionUtil.and(guard, symbol.getPrecondition());
    }
    return guard;
  }
}
