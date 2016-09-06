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
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.transitionSystem.SymbolicTransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.TransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystem;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystemLoader;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author falk
 */


public class Main {
    
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: stateexplorer [file]");
            System.out.println("  [file] - file with transition system");
            return;
        }
        
        TransitionSystem system = TransitionSystemLoader.load(args[0]);
        
        Properties conf = new Properties();
        conf.setProperty("symbolic.dp", "Z3");
        conf.setProperty("symbolic.dp.z3.bitvectors", "false");
        conf.setProperty("log.finest", "psyco");
        ConstraintSolverFactory factory = new ConstraintSolverFactory(conf);
        ConstraintSolver solver = factory.createSolver();      
        
        TransitionHelper symbolicHelper = new SymbolicTransitionHelper();
        system.setHelper(symbolicHelper);
        SearchIterationImage image = 
                SymbolicSearchEngine.symbolicBreadthFirstSearch(system,
                        solver, Integer.MIN_VALUE);        
        
        image.print(System.out);
        
        System.out.println("### done ###");
    } 
    
}
