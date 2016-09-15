/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.jstateexplorer.transitionSystem;

import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.datastructures.state.State;
import java.util.Iterator;

/**
 *
 * @author mmuesly
 */
public class SynchronisedTransitionHelper extends SymbolicTransitionHelper {

  public SynchronisedTransitionHelper(){
    
  }

  @Override
  public SearchIterationImage applyTransition(SearchIterationImage image, Transition transition) {
    if(transition.isError() && transition.isOk()){
        for (Iterator it = image.getPreviousNewStates().values().iterator(); it.hasNext();) {
            State state = (State) it.next();
            if(super.satisfiesGuardCondition(state, transition, image.getDepth())){
              TransitionMonitor.stopRuning();
              System.out.println("gov.nasa.jstateexplorer.transitionSystem.SynchronisedTransitionHelper.applyTransition() -History");
              System.out.println(transition.toStringWithId());
              System.out.println("State: " + state.toExpression().toString());
              System.out.println("History: " + state.getHistoryAsString());
              state.addToHistory(transition);
              image.setHistoryForCE(state.getHistory());
              return image;
            }
        }
        return image;
    }else{
        return super.applyTransition(image, transition);
    }
  }
}
