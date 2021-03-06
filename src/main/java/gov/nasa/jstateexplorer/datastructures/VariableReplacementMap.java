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
package gov.nasa.jstateexplorer.datastructures;

import com.google.common.base.Function;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.HashMap;
/**
 * The VariableReplacementMap can be used by the VariableReplacementVisitor to 
 * replace a variable by a value Expression.
 * @author mmuesly
 */
public class VariableReplacementMap
        extends HashMap<Variable, Expression<Boolean>>
        implements Function<Variable<?>, Expression<Boolean>>{

  @Override
  public Expression<Boolean> apply(Variable<?> variable) {
    return getOrDefault(variable, (Expression) variable);
  }
}