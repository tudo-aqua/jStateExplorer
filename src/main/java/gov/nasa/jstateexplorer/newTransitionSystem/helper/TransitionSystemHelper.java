package gov.nasa.jstateexplorer.newTransitionSystem.helper;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemHelper {
  
  public static Expression<Boolean> createReachExpression(
          TransitionSystem system){
    Expression reachedStates = null;
    HashMap<Integer, List<SymbolicState>> states = system.getAllStates();
    for(List<SymbolicState> statesInOneDepth: states.values()){
      for(SymbolicState state: statesInOneDepth){
        reachedStates = ((reachedStates == null)? state.toExpression():
                ExpressionUtil.and(reachedStates, state.toExpression()));
      }
    }
    return reachedStates;
  }
}
