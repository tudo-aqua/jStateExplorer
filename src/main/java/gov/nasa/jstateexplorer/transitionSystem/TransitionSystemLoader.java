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

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.exceptions.ImpreciseRepresentationException;
import gov.nasa.jpf.constraints.expressions.BitvectorExpression;
import gov.nasa.jpf.constraints.expressions.BitvectorNegation;
import gov.nasa.jpf.constraints.expressions.BitvectorOperator;
import gov.nasa.jpf.constraints.expressions.CastExpression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.UnaryMinus;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.ConcreteType;
import gov.nasa.jstateexplorer.transitionSystem.helperVisitors.TransitionEncoding;
import gov.nasa.jstateexplorer.util.HelperMethods;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
/**
 * The transition system loader is able to parse a transition 
 * system and load it from a file. 
 * So the search can be executed without a jDart run in upfront.
 */
public class TransitionSystemLoader {

  public String fileName;
  public String currentLine;
  private Logger logger = Logger.getLogger(HelperMethods.getLoggerName());

  public TransitionSystemLoader(String fileName) {
    this.fileName = fileName;
  }

  public TransitionSystem parseFile() throws ImpreciseRepresentationException{
    try (BufferedReader reader = 
            new BufferedReader(new FileReader(fileName))) {
      TransitionSystem tSystem = new TransitionSystem(null);
      String line;
      while ((line = reader.readLine()) != null) {
        currentLine = line;
        if (nextTokenIs(TransitionEncoding.valuation)) {
          Valuation initValuation = parseInitialValuation();
          tSystem.setInitValuation(initValuation);
        }
        if (nextTokenIs(TransitionEncoding.okTransition)) {
          Transition t = parseOkTransition();
          tSystem.add(t);
        }
        if (nextTokenIs(TransitionEncoding.errorTransition)) {
          Transition t = parseErrorTransition();
          tSystem.add(t);
        }
      }
      return tSystem;
    } catch (IOException ex) {
      logger.severe(ex.toString());
    }
    return null;
  }

  private Transition parseErrorTransition() throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    Expression guard = null;
    if (nextTokenIs(TransitionEncoding.guard)) {
      guard = parseGuard();
    }
    currentLine = currentLine.substring(4);
    int index = currentLine.indexOf(':');
    String error = currentLine.substring(0, index);
    currentLine = currentLine.substring(index + 1);
    index = currentLine.indexOf(":");
    String stackTrace = currentLine.substring(0, index);
    currentLine = currentLine.substring(index + 1);
    Map<Variable, Expression<Boolean>> post = new HashMap<>();
    while (nextTokenIs(TransitionEncoding.effect)) {
        post = parseTransitionEffect(post);
    }
    currentLine = currentLine.substring(0 + 1).replace("\n", "");
    if (!currentLine.equals(";;")) {
      System.err.println("gov.nasa.jpf.psyco.search.transitionSystem"
              + ".TransitionSystemLoader.parseErrorTransition()");
      System.err.println(currentLine);
      throw new IllegalStateException(
              "The error Transition is not parsed corretly");
    }
    if(error.equals("")){
      error = null;
    }
    if(stackTrace.equals("")){
      stackTrace = null;
    }
    return new Transition(guard, error, stackTrace, post, false, true);
  }

  private Transition parseOkTransition() throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    Expression guard;
    if (nextTokenIs(TransitionEncoding.guard)) {
      guard = parseGuard();
    } else {
      throw new IllegalStateException("The input file is malformed."
              + "Missign Guard in Transition");
    }
    Map<Variable, Expression<Boolean>> post = new HashMap<>();
    if (nextTokenIs(TransitionEncoding.transitionBody)) {
      currentLine = currentLine.substring(2);

      while (nextTokenIs(TransitionEncoding.effect)) {
        post = parseTransitionEffect(post);
      }
      if (!currentLine.replace("\n", "").equals(";;")) {
        throw new IllegalStateException(
                "The transition Line cannot be parsed entirely");
      }
    }
    return new Transition(guard, post, true, false);
  }

  private Map<Variable, Expression<Boolean>> parseTransitionEffect(Map<Variable, Expression<Boolean>> post) throws ImpreciseRepresentationException{
    currentLine = currentLine.substring(2);
    Variable effectedVar = null;
    if (nextTokenIs(TransitionEncoding.variable)) {
      effectedVar = parseVariable();
      currentLine = currentLine.substring(1);
    }
    Expression effect = parseNextExpression();
    if (currentLine.startsWith(";")) {
      currentLine = currentLine.substring(1);
    }
    post.put(effectedVar, effect);
    return post;
  }

  private BitvectorExpression parseBitVectorExpression()  throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    Expression left = parseNextExpression();
    currentLine = currentLine.substring(1);
    BitvectorOperator operator;
    if (nextTokenIs(TransitionEncoding.bitVectorOperator)) {
      currentLine = currentLine.substring(1);
      int operatorEnde = currentLine.indexOf(";");
      String readOperator = currentLine.substring(0, operatorEnde);
      operator = BitvectorOperator.valueOf(readOperator);
      currentLine = currentLine.substring(operatorEnde + 1);
    } else {
      throw new IllegalStateException(
              "The next token must be an BitVectorOperator");
    }
    currentLine = currentLine.substring(1);
    Expression right = parseNextExpression();
    return new BitvectorExpression(left, operator, right);
  }

  private BitvectorNegation parseBitvectorNeagtion()  throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    Expression negated = parseNextExpression();
    currentLine = currentLine.substring(1);
    return new BitvectorNegation(negated);
  }

  private Constant parseConstant() throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    int endValue = currentLine.indexOf(':');
    String value = currentLine.substring(0, endValue);
    currentLine = currentLine.substring(endValue + 1);
    endValue = currentLine.indexOf(';');
    String type = currentLine.substring(0, endValue);
    currentLine = currentLine.substring(endValue + 1);
    return Constant.createParsed(resolveType(type), value);
  }

  private Negation parseNegation()  throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    Expression negated = parseNextExpression();
    currentLine = currentLine.substring(1);
    return new Negation(negated);
  }

  private NumericBooleanExpression parseNumericBooleanExpression() throws ImpreciseRepresentationException  {
    currentLine = currentLine.substring(2);
    Expression left = parseNextExpression();
    currentLine = currentLine.substring(3);
    int endOperator = currentLine.indexOf(';');
    String operator = currentLine.substring(0, endOperator);
    NumericComparator op = NumericComparator.fromString(operator);
    currentLine = currentLine.substring(endOperator + 2);
    Expression right = parseNextExpression();
    currentLine = currentLine.substring(1);
    return new NumericBooleanExpression(left, op, right);
  }

  private NumericCompound parseNumericCompound() throws ImpreciseRepresentationException  {
    currentLine = currentLine.substring(2);
    Expression left = parseNextExpression();
    currentLine = currentLine.substring(3);
    int endOperator = currentLine.indexOf(';');
    String operator = currentLine.substring(0, endOperator);
    NumericOperator op = NumericOperator.fromString(operator);
    currentLine = currentLine.substring(endOperator + 2);
    Expression right = parseNextExpression();
    currentLine = currentLine.substring(1);
    return new NumericCompound(left, op, right);
  }

  private PropositionalCompound parsePropositionalCompound() throws ImpreciseRepresentationException  {
    currentLine = currentLine.substring(2);
    Expression left = parseNextExpression();
    currentLine = currentLine.substring(3);
    int endOperator = currentLine.indexOf(';');
    String operator = currentLine.substring(0, endOperator);
    LogicalOperator op = LogicalOperator.fromString(operator);
    currentLine = currentLine.substring(endOperator + 2);
    Expression right = parseNextExpression();
    currentLine = currentLine.substring(1);
    return new PropositionalCompound(left, op, right);
  }

  private UnaryMinus parseUnaryMinus() throws ImpreciseRepresentationException  {
    currentLine = currentLine.substring(2);
    Expression unaryExpression = parseNextExpression();
    currentLine = currentLine.substring(1);
    return UnaryMinus.create(unaryExpression);
  }

  private Valuation parseInitialValuation() throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    Valuation result = new Valuation();
    while (nextTokenIs(TransitionEncoding.valuationEntry)) {
      currentLine = currentLine.substring(2);
      Variable var = parseVariable();
      currentLine = currentLine.substring(1);
      int endValue = currentLine.indexOf(';');
      String value = currentLine.substring(0, endValue);
      currentLine = currentLine.substring(endValue + 1);
      ValuationEntry entry
              = new ValuationEntry(var, var.getType().parse(value));
      result.addEntry(entry);
    }
    currentLine = currentLine.substring(1).replace("\n", "");
    if (!currentLine.isEmpty()) {
      throw new IllegalStateException("Valuation not finished");
    }
    return result;
  }

  public static TransitionSystem load(String fileName) throws ImpreciseRepresentationException {
    TransitionSystemLoader loader = new TransitionSystemLoader(fileName);
    return loader.parseFile();
  }

  private Expression parseGuard() throws ImpreciseRepresentationException  {
    currentLine = currentLine.substring(2);
    Expression guard = parseNextExpression();
    currentLine = currentLine.substring(1);
    return guard;
  }

  private Variable parseVariable() throws ImpreciseRepresentationException  {
    currentLine = currentLine.substring(2);
    int endName = currentLine.indexOf(':');
    String name = currentLine.substring(0, endName);
    currentLine = currentLine.substring(endName);
    endName = currentLine.indexOf(';');
    String type = currentLine.substring(1, endName);
    currentLine = currentLine.substring(endName + 1);
    return Variable.create(resolveType(type), name);
  }

  private boolean nextTokenIs(char tokenType) {
    String prefix = String.valueOf(tokenType);
    return currentLine.startsWith(prefix);
  }

  private ConcreteType resolveType(String encodedType) {
    switch(encodedType){
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$SInt32Type":
        return BuiltinTypes.SINT32;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$SInt16Type":
        return BuiltinTypes.SINT16;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$SInt8Type":
        return BuiltinTypes.SINT8;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$SInt64Type":
        return BuiltinTypes.SINT64;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$BoolType":
        return BuiltinTypes.BOOL;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$DecimalType":
        return BuiltinTypes.DECIMAL;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$DoubleType":
        return BuiltinTypes.DOUBLE;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$FloatType":
        return BuiltinTypes.FLOAT;
      case "gov.nasa.jpf.constraints.types.BuiltinTypes$UInt16Type":
        return BuiltinTypes.UINT16;
      default:
        System.err.println("gov.nasa.jstateexplorer.transitionSystem.TransitionSystemLoader.resolveType()");
        System.err.println(encodedType);
        System.err.flush();
        throw new IllegalStateException("This Type is unknown: " + encodedType);
    }
  }
  private Expression parseNextExpression() throws ImpreciseRepresentationException {
    char nextExpressionType = currentLine.charAt(0);
    switch (nextExpressionType) {
      case TransitionEncoding.bitVector:
        return parseBitVectorExpression();
      case TransitionEncoding.bitVectorNegation:
        return parseBitvectorNeagtion();
      case TransitionEncoding.constant:
        return parseConstant();
      case TransitionEncoding.negation:
        return parseNegation();
      case TransitionEncoding.numericBooleanExpression:
        return parseNumericBooleanExpression();
      case TransitionEncoding.numericCompund:
        return parseNumericCompound();
      case TransitionEncoding.propositionalCompound:
        return parsePropositionalCompound();
      case TransitionEncoding.unaryMinus:
        return parseUnaryMinus();
      case TransitionEncoding.variable:
        return parseVariable();
      case TransitionEncoding.cast:
        return parseCastExpression();
      default:
        System.err.println("gov.nasa.jpf.psyco.search.transitionSystem"
                + ".TransitionSystemLoader.parseNextExpression()");
        System.err.println("line: " + currentLine);
        throw new IllegalStateException("Cannot parse next Expression.");
    }
  }

  private Expression parseCastExpression()  throws ImpreciseRepresentationException {
    currentLine = currentLine.substring(2);
    Expression casted = parseNextExpression();
    currentLine = currentLine.substring(1);
    int index = currentLine.indexOf(";");
    String type = currentLine.substring(0, index);
    currentLine = currentLine.substring(index +1);
    return CastExpression.create(casted, resolveType(type));
  }
}
