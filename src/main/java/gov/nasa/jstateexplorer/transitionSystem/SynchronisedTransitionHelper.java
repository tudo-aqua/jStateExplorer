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

import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.datastructures.state.State;
import java.util.Iterator;

public class SynchronisedTransitionHelper extends SymbolicTransitionHelper {

  public SynchronisedTransitionHelper(){
    
  }

  @Override
  public SearchIterationImage applyTransition(SearchIterationImage image, Transition transition) {
    if(transition.isError() && transition.isOk()){
        for (Iterator it = image.getPreviousNewStates().values().iterator(); it.hasNext();) {
            State state = (State) it.next();
            logger.finer("gov.nasa.jstateexplorer.transitionSystem.SynchronisedTransitionHelper.applyTransition()");
            logger.finer("transition: " + transition.toStringWithId());
            logger.finer("state" + state.toExpression());
            boolean satisfiabel = super.satisfiesGuardCondition(state, transition, image.getDepth());
            logger.finer("guardCheck: " + satisfiabel);
            if(satisfiabel){
              TransitionMonitor.stopRuning();
              logger.finer("gov.nasa.jstateexplorer.transitionSystem.SynchronisedTransitionHelper.applyTransition() -History");
              logger.finer(transition.toStringWithId());
              logger.finer("State: " + state.toExpression().toString());
              state.addToHistory(transition);
              logger.finer("History: " + state.getHistoryAsString());
              logger.finer("TransitionMonitor: " + TransitionMonitor.isRunning());
              image.setHistoryForCE(state.getHistory());
              return image;
            }
        }
        return image;
    }else{
        return super.applyTransition(image, transition);
    }
  }
  
  @Override
  public boolean shouldContinue(SearchIterationImage image){
    return image.getHistoryForCE().isEmpty();
  }
}
