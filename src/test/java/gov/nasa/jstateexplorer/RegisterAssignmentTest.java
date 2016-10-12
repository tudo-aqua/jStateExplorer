package gov.nasa.jstateexplorer;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.transitionSystem.SymbolicTransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.Transition;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystem;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class RegisterAssignmentTest {
  ConstraintSolver solver;
  
  public RegisterAssignmentTest(){
  }
  
  @Before
  public void setUp() {
    Properties conf = new Properties();
    conf.setProperty("symbolic.dp", "Z3");
    conf.setProperty("symbolic.dp.z3.bitvectors", "false");
    conf.setProperty("log.finest", "psyco");
    ConstraintSolverFactory factory = new ConstraintSolverFactory(conf);
    solver = factory.createSolver();
    SearchConfig pconf = new SearchConfig();
  }
  
  @Test
  public void testRegisterAssignemnt() throws IOException{
    SymbolicTransitionHelper helper = new SymbolicTransitionHelper();
    TransitionSystem system = createTransitionSystem();
    system.setHelper(helper);
    Logger logger = Logger.getLogger("psyco");
    logger.setLevel(Level.FINE);
    logger.info("start Test");
    logger.info(system.completeToString());
    SearchIterationImage searchResult = SymbolicSearchEngine.
            symbolicBreadthFirstSearch(system, solver, 10, logger);
    logger.info("searchDone");
    StringBuilder searchResultString = new StringBuilder();
    searchResult.print(searchResultString);
    logger.info(searchResultString.toString());
    assertEquals(4, searchResult.getDepth());
  }
  
  private TransitionSystem createTransitionSystem(){
    Variable x1 = new Variable(BuiltinTypes.SINT32, "this.x_1");
    Variable x2 = new Variable(BuiltinTypes.SINT32, "this.x_2");
    Variable y = new Variable(BuiltinTypes.SINT32, "this.y");
    Constant<Integer> constant5 = Constant.create(BuiltinTypes.SINT32, 5);
    Constant<Integer> constant1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant constant15 = Constant.create(BuiltinTypes.SINT32, 15);
    Variable p1 = new Variable(BuiltinTypes.SINT32, "input");
    Expression guard1 = NumericBooleanExpression.create(x1,
            NumericComparator.GT, constant1);
    Expression effect = 
            p1;
    Expression effect1 =
            constant15;
    Map<Variable, Expression<Boolean>> post = new HashMap<>();
    post.put(x1, effect);
    post.put(y, effect1);
    Transition t1 = new Transition(guard1, post, true, false);
    Expression effect2 = 
            x1;
    post = new HashMap<>();
    post.put(x2, effect2);
    Expression guard2 = NumericBooleanExpression.create(y, NumericComparator.GE, constant15);
    Transition t2 = new Transition(guard2, post, true, false);
    Valuation initValuation = new Valuation();
    initValuation.addEntry(new ValuationEntry(x1,3));
    initValuation.addEntry(new ValuationEntry(x2,0));
    initValuation.addEntry(new ValuationEntry(y,2));
    TransitionSystem system = new TransitionSystem();
    system.setInitValuation(initValuation);
    system.add(t1);
    system.add(t2);
    return system;
  }
}
