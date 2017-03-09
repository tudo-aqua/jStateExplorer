package gov.nasa.jstateexplorer.newTransitionSystem.helper;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mmuesly
 */
public class TransitionSystemHelper {
  
  public static Expression<Boolean> createReachExpression(
          TransitionSystem system){
    Expression reached = null;
    HashMap<Integer, List<SymbolicState>> states = system.getAllStates();
    for(List<SymbolicState> statesInOneDepth: states.values()){
      for(SymbolicState state: statesInOneDepth){
        System.out.println("State:");
        System.out.println(state);
        if(!state.isError()){
          reached = addStateToReachedStates(reached, state);
        }
      }
    }
    return reached;
  }
  private static Expression<Boolean> addStateToReachedStates(
          Expression reached, SymbolicState state){
    Expression stateExpression = state.toExpression();
    stateExpression = ExpressionQuantifier.existensQuantification(
                          stateExpression, state.keySet());
    reached = ((reached == null)? stateExpression:
                  ExpressionUtil.or(reached, stateExpression));
    return reached;
  }
  
}
