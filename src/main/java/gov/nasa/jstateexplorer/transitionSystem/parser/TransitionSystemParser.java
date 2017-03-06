package gov.nasa.jstateexplorer.transitionSystem.parser;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.parser.ParserUtil;
import gov.nasa.jpf.constraints.types.TypeContext;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionLabel;
import gov.nasa.jstateexplorer.newTransitionSystem.TransitionSystem;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.antlr.runtime.RecognitionException;

/**
 * This is a first version for a TransitionSystemParser. Mayber it should be 
 * replaced by a parser based on ANTLR once the Grammar is complete.
 * @author mmuesly
 */
public class TransitionSystemParser {


  boolean declaration, precondition, newTransition, effect, transitionParameter;
  int count;
  TransitionLabel currentTransition;
  TransitionSystem system;
  ParserUtil jConstraintParser;
  
  /*
   *A few Symbols are required to define a TransitionSystem in a File.
   *This Symbols must be declared as static final String.
   */
  public static final String preconditionSymbol = "PRECONDITION";
  public static final String transitionParameterSymbol = "PARAMETER";
  public static final String variableSymbol = "VARIABLES";
  public static final String effectSymbol = "EFFECT";
  public static final String transitionSymbol = "TRANSITION";
  public static String errorSymbol = "ERROR";

  public TransitionSystemParser(){
    setToDefault();
    jConstraintParser = new ParserUtil();
    this.system = new TransitionSystem();
    this.currentTransition = null;
    this.count = 0;
    
  }
  
  public TransitionSystem parseString(String input) throws RecognitionException {
    String[] lines = input.split("\n");
    for(String line: lines){
      parseLine(line);
    }
    return getFinalSystem();
  }

  private void parsedTransitionSymbol(String line){
    setToDefault();
    newTransition = true;
    String[] tokens = line.split(" ");
    String name = "";
    if(tokens.length > 1){
      name = tokens[1].replace(":", "");
    }else{
      name = "t_" + count;
      ++count;
    }
    if(currentTransition != null){
      system.addTransitionLabel(currentTransition);
    }
    currentTransition = new TransitionLabel(name, system.getStateVariables());
    
  }
  
  private void setToDefault(){
    this.declaration = false;
    this.precondition = false;
    this.newTransition = false;
    this.transitionParameter = false;
    this.effect = false;
  }

  private void parseEffectLine(String line) 
          throws RecognitionException, TransitionSystemParserError {
    if(line.contains(":")){
      String effectedVariableName = 
              TransitionSystemParserHelper.extractEffectedVariableName(line);
      if(system.hasStateVariableWithName(effectedVariableName)){
        Collection<Variable<?>> expectedVariables = 
                collectExpectedVariablesInEffect(effectedVariableName);
        String effectString = TransitionSystemParserHelper.extractEffect(line);
        System.out.println("reproducded Effect: " + effectString);
        Expression<Boolean> effect = 
                ParserUtil.parseLogical(
                        effectString, getTypeContext(), expectedVariables);
        currentTransition.addEffect(effectedVariableName, effect);
      }else{
        throw new TransitionSystemParserError(line,
                "Expected that modified Variable: " 
                  + effectedVariableName 
                  + " is a state variable,\n"
                  + "but could not find the corresponding state variable.");
      }
    }else if(line.startsWith(TransitionSystemParser.errorSymbol)){
      currentTransition.markAsErrorTransitionLabel();
    }
    else{
      throw new TransitionSystemParserError(line, 
              "Expected an Effect in Form var: jConstraintExpression,\n"
              +"but could not find a colon in the line.");
    }
  }

  private void parsePreconditionLine(String line) throws RecognitionException {
    Expression<Boolean> preconditionPart = 
            ParserUtil.parseLogical(line, 
                    getTypeContext(), collectExpectedVariableInPrecondition());
    currentTransition.addPrecondition(preconditionPart);
  }

  private void parseDeclarationLine(String line) throws RecognitionException {
    List<Variable<?>> declaredVariables = ParserUtil.parseVariableDeclaration(line);
    system.addVariables(declaredVariables);
    return;
  }

  private void parseLine(String line) throws RecognitionException {
     String uppercaseLine = line.toUpperCase();
      if(this.newTransition 
              && !line.startsWith(TransitionSystemParser.preconditionSymbol)
              && !line.startsWith(transitionParameterSymbol)
              && !line.startsWith(TransitionSystemParser.effectSymbol)){
        return;
      }
      if(uppercaseLine.startsWith(TransitionSystemParser.variableSymbol)) {
          parsedVariableSymbol();
          return;
      }
      if(uppercaseLine.startsWith(TransitionSystemParser.transitionSymbol)) {
        parsedTransitionSymbol(line);
        return;
      }
      if(uppercaseLine.startsWith(
              TransitionSystemParser.transitionParameterSymbol)) {
        parsedTransitionParameter(line);
        return;
      }
      if(line.startsWith(TransitionSystemParser.preconditionSymbol)
              && (this.newTransition || this.transitionParameter)) {
        parsedPreconditionSymbol();
        return;
      }
      if(line.startsWith(TransitionSystemParser.effectSymbol)){
        parsedEffectSymbol();
        return;
      }
      if(this.precondition){
        parsePreconditionLine(line);
        return;
      }
      if(this.effect){
        parseEffectLine(line);
        return;
      }
      if(this.declaration){
        parseDeclarationLine(line);
        return;
      }
      if(this.transitionParameter){
        parseParameterLine(line);
      }
  }

  private void parsedEffectSymbol() {
    this.newTransition = false;
    this.precondition = false;
    this.effect = true;
    this.transitionParameter = false;
  }

  private void parsedPreconditionSymbol() {
    this.newTransition = false;
    this.effect = false;
    this.precondition = true;
    this.transitionParameter = false;
  }

  private void parsedVariableSymbol() {
    setToDefault();
    this.declaration = true;
  }
  
  private TypeContext getTypeContext() {
    return new TypeContext(true);
  }

  private Collection<Variable<?>> collectExpectedVariableInPrecondition() {
    Collection expectedVariables = currentTransition.getParameterVariables();
    expectedVariables.addAll(system.getStateVariables());
    return expectedVariables;
  }

  private Collection<Variable<?>> collectExpectedVariablesInEffect(
          String effectedVariableName) {
    Variable oldVar = system.getStateVariableByName(effectedVariableName);
    Collection<Variable<?>> expectedVariables = system.getStateVariables();
    Collection<Variable<?>> parameterVariables =
            currentTransition.getParameterVariables();
    expectedVariables.addAll(parameterVariables);
    Variable primeVar = 
            new Variable(oldVar.getType(), effectedVariableName +"'");
    expectedVariables.add(primeVar);
    return expectedVariables;
  }

  /*
  * Before exiting the parser all pending open transitions must be added 
  * to the system. This is done in this method.
  */
  private TransitionSystem getFinalSystem(){
    if(currentTransition != null){
      system.addTransitionLabel(currentTransition);
    }
    return system;
  }
  TransitionSystem parseFile(String fileName) throws FileNotFoundException, IOException, RecognitionException {
    BufferedReader inputFile = new BufferedReader(new FileReader(fileName));
    String line;
    while((line = inputFile.readLine()) != null){
      parseLine(line);
    }
    
    return getFinalSystem();
  }

  private void parsedTransitionParameter(String line) {
    this.newTransition = false;
    this.precondition = false;
    this.effect = false;
    this.transitionParameter = true;
  }

  private void parseParameterLine(String line) throws RecognitionException {
    Collection<Variable<?>> parameters =
            ParserUtil.parseVariableDeclaration(line);
    for(Variable parameter: parameters){
      currentTransition.addParameterVariable(parameter);
    }
  }
}
