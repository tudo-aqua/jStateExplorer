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
package gov.nasa.jstateexplorer.datastructures.region;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.datastructures.state.State;
import java.io.IOException;
import java.util.HashMap;

/**
 * A Region is a set of states.
 * @param <E> Any class extending ValuationEntry
 * @param <T> Any class extending a State description.
 */
public abstract class Region<E extends ValuationEntry, T extends State<E>>
        extends HashMap<String, T> {

  public Region() {
  }

  ;

  public Region(Valuation initValuation) {
    addInitialValuation(initValuation);
  }

  public Expression<Boolean> toExpression() {
    Expression returnExpression = null;
    for (T state : values()) {
      Expression stateExpression = state.toExpression();
      returnExpression
              = returnExpression == null
                      ? stateExpression
                      : ExpressionUtil.or(stateExpression, returnExpression);
    }
    return returnExpression;
  }

  public void print(Appendable a) throws IOException {
    for (String stateName : keySet()) {
      a.append("stateName: ");
      a.append(stateName);
      a.append("\n");
      for (E entry : get(stateName)) {
        a.append(entry.getVariable().getName());
        a.append(": ");
        a.append(entry.getValue().toString());
        a.append("\n");
      }
      a.append("\n");
    }
  }

  public abstract void addInitialValuation(Valuation initValuation);

  public abstract Region createNewRegion();
}