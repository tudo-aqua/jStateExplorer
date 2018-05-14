package gov.nasa.jstateexplorer.transitionSystem;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.ExpressionConverterVisitor;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.TransitionEncoding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Transition {

  private Expression guard, transition;
  private Map<Variable, Expression<Boolean>> effects;
  private String errorMessage;
  private String stackTrace;
  private List<Variable> stateVariables;
  private static Logger logger;
  private boolean isReached;
  private String id;
  private boolean isOk;
  private boolean isError;
  private List<Variable> modified;
  private boolean isConstructor;

  public Transition() {
    guard = null;
    effects = new HashMap<>();
    stateVariables = new ArrayList<>();
    logger = Logger.getLogger("psyco");
    isReached = false;
    id = null;
    isOk = false;
    isError = false;
  }

  public Transition(Expression guard, Map<Variable, Expression<Boolean>> effects) {
    this();
    this.guard = guard;
    setEffects(effects);
    calculateModified();
  }

  public Transition(Expression guard, Map<Variable, Expression<Boolean>> effects, String id) {
    this(guard, effects);
    this.id = id;
  }

  public Transition(Expression guard, Map<Variable, Expression<Boolean>> effects, boolean ok, boolean error) {
    this(guard, effects);
    this.isError = error;
    this.isOk = ok;
  }

  public Transition(Expression guard, Map<Variable, Expression<Boolean>> effects, String id, boolean ok, boolean error, boolean constructor) {
    this(guard, effects, ok, error);
    this.isConstructor = constructor;
    this.id = id;
  }

  public Transition(Expression guard, String errorMsg, String stackTrace, boolean ok, boolean error) {
    this(guard, null, ok, error);
    this.errorMessage = errorMsg;
    this.stackTrace = stackTrace;
  }

  public Transition(Expression guard, String errorMsg, String stackTrace, String id, boolean ok, boolean error, boolean constructor) {
    this(guard,errorMsg, stackTrace, ok, error);
    this.isConstructor = constructor;
    this.id = id;
  }
  
  public Transition(Expression guard, String errorMsg, String stackTrace, Map<Variable, Expression<Boolean>> effects, boolean ok, boolean error) {
    this(guard, errorMsg, stackTrace, ok, error);
    setEffects(effects);
    calculateModified();
  }
  
  public Transition(Expression guard, String errorMsg, String stackTrace, Map<Variable, Expression<Boolean>> effects, boolean ok, boolean error, boolean constructor) {
    this(guard, errorMsg, stackTrace, effects, ok, error);
    this.isConstructor = constructor;
  }
  
  public Transition(Expression guard, String errorMsg, String stackTrace, String id, boolean ok, boolean error) {
    this(guard, errorMsg, stackTrace, ok, error);
    this.id = id;
  }

  public Transition(Expression guard, Map<Variable, Expression<Boolean>> effects, String id, boolean ok, boolean error) {
    this(guard, effects, ok, error);
    this.id = id;
  }

  public Expression getGuard() {
    return guard;
  }

  public void setGuard(Expression guard) {
    this.guard = guard;
  }

  public boolean isGuardSymbolicConstant() {
    return ExpressionUtil.isTrue(guard) || ExpressionUtil.isFalse(guard);
  }

  public Map<Variable, Expression<Boolean>> getEffects() {
    return effects;
  }

  public Expression getEffect(Variable var) {
    return effects.getOrDefault(var, var);
  }

  public void setEffects(Map<Variable, Expression<Boolean>> effects) {
    if (effects != null) {
      this.effects = effects;
      stateVariables = new ArrayList(this.effects.keySet());
      transition = null;
    } 
  }

  public boolean isReached() {
    return isReached;
  }

  public void setReached(boolean isReached) {
    this.isReached = isReached;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isOk() {
    return isOk;
  }

  public void setOk(boolean isOk) {
    this.isOk = isOk;
  }

  public boolean isError() {
    return isError;
  }

  public void setError(boolean isError) {
    this.isError = isError;
  }

  public boolean isStutterTransition() {
    if(isOk()){
      return modified.isEmpty();
    }
    return false;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }

  public List<Variable> getStateVariables(){
    return this.stateVariables;
  }

  public SearchIterationImage applyOn(SearchIterationImage currentState, TransitionHelper helper) {
    return helper.applyTransition(currentState, this);
  }

  @Override
  public String toString() {
    if (isError()) {
      return toStringError();
    }else if (isOk()) {
      return toStringOk();
    } else {
      String msg = "This Transition is neither ok nor erroneous.";
      throw new RuntimeException(msg);
    }
  }

  public String toStringWithId() {
    String returnValue = "id: " + id + ": ";
    if (isOk() && isError()) {
      return returnValue + "OK/ERROR" + toStringOk();
    } else if(isOk()) { 
      return returnValue + toStringOk();
    }else if (isError()) {
      return returnValue + toStringError();
    } else {
      String msg = "This Transition is neither ok nor erroneous.";
      throw new RuntimeException(msg);
    }
  }

  private String toStringOk() {
    StringBuilder builder = new StringBuilder();
    builder.append("[OK]: ");
    builder.append(guard.toString());
    builder.append(" / ");
    builder.append(effects.toString());
    builder.append("\n");
    return builder.toString();
  }

  private String toStringError() {
    StringBuilder builder = new StringBuilder();
    builder.append("[Error]: ");
    builder.append(guard.toString());
    builder.append(" / ");
    if (errorMessage != null) {
      builder.append(errorMessage);
      builder.append(" / ");
    }
    if (stackTrace != null) {
      builder.append(stackTrace);
      builder.append(" / ");
    }
    if (!effects.isEmpty()) {
      builder.append("[ ");
      for (Variable key : stateVariables) {
        Expression effect = effects.get(key);
        builder.append(key.getName());
        builder.append(": ");
        builder.append(effect.toString());
        builder.append(", ");
      }
      builder.append(" ]");
    }
    builder.append("\n");
    return builder.toString();
  }

  public Expression convertToExpression() {
    if (transition == null) {
      transition = guard;
      for (Variable key : stateVariables) {
        Expression resultingExpression = effects.get(key);
        Variable newKey = new Variable(key.getType(), key.getName() + "'");
        resultingExpression = NumericBooleanExpression.create(
                newKey,
                NumericComparator.EQ,
                resultingExpression);
        transition = ExpressionUtil.and(transition,
                resultingExpression);
      }
    }
    return transition;
  }

  private void calculateModified() {
    modified = new ArrayList<>();
    for (Variable variable : effects.keySet()) {
      Expression value = effects.get(variable);
      if (value instanceof Variable && value.equals(variable)) {
        continue;
      }
      modified.add(variable);
    }
  }

  public String convertForFile(HashMap<Class, String> data){
    ExpressionConverterVisitor expressionConverter = 
            new ExpressionConverterVisitor();
    String guardForFile = 
            (String) guard.accept(expressionConverter, data);
    String effectsForFile;
    if(isOk()){
      effectsForFile = convertEffectsForFile(data);
      return TransitionEncoding.okTransition + ":" 
              + TransitionEncoding.guard + ":" + guardForFile + ";" 
              + TransitionEncoding.transitionBody + ":" + effectsForFile + ";" + ";";
    }else{
      effectsForFile = convertErrorTransitiontForFile(data);
      return TransitionEncoding.errorTransition + ":" 
              + TransitionEncoding.guard + ":" + guardForFile + ":" 
              + TransitionEncoding.transitionBody + ":" + effectsForFile + ";" + ";";
    }
  }

  public void setIsConstructor(boolean value){
      this.isConstructor = value;
  }
  public boolean isConstructor(){
      return this.isConstructor;
  }
  private String convertErrorTransitiontForFile(HashMap<Class, String> data) {
    String errorTransitionForFile = TransitionEncoding.error + ":";
    errorTransitionForFile += (errorMessage == null?"":errorMessage);
    errorTransitionForFile += ":" + (stackTrace == null?"":stackTrace);
    errorTransitionForFile += ":" + convertEffectsForFile(data) + ";";
    return errorTransitionForFile; 
  }

  private String convertEffectsForFile(HashMap<Class, String> data){
    ExpressionConverterVisitor expressionConverter = 
            new ExpressionConverterVisitor();
    String effectsForFile = "";
    for(Variable var: effects.keySet()){
      String key = (String) var.accept(expressionConverter, data);
      String effect = 
            (String) effects.get(var).accept(expressionConverter, data);
      effectsForFile += TransitionEncoding.effect + ":" + key + ":" + effect + ";";
    }
    return effectsForFile;
  }
}
