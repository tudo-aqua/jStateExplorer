package gov.nasa.jstateexplorer.newTransitionSystem.helper;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.datastructures.NameMap;
import gov.nasa.jstateexplorer.newDatastructure.SymbolicState;
import java.util.Collection;
import java.util.Set;


/**
 *
 * @author mmuesly
 */
public class RenameUtils {

  public static SymbolicState rename(SymbolicState resultingState,
          Collection<Variable<?>> parameters, long id) {
     NameMap renamings = createRenamingMap(resultingState.keySet(), parameters, id);
     for(Variable stateVariable: resultingState.keySet()){
       Expression value = resultingState.get(stateVariable);
       value = ExpressionUtil.renameVars(value, renamings);
       NameMap primeRename = new NameMap();
       primeRename.mapNames(
               stateVariable.getName() +"'", stateVariable.getName());
       value = ExpressionUtil.renameVars(value, primeRename);
       resultingState.put(stateVariable, value);
    }
    return resultingState;
  }

  private static NameMap createRenamingMap(
          Set<Variable<?>> stateVariables,
          Collection<Variable<?>> parameters, long id) {
    NameMap renamings = new NameMap();

    //State Variable Renamings:
    for(Variable stateVariable: stateVariables){
      String replacementName = stateVariable.getName() + "_sv_" + id;
      renamings.mapNames(stateVariable.getName(), replacementName);
    }
    //Parameter Renamings
    for(Variable parameter: parameters){
      String replacementName = parameter.getName() + "_p_" + id;
      renamings.mapNames(parameter.getName(), replacementName);
    }
    return renamings;
  }
}
