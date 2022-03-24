package gov.nasa.jstateexplorer;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import java.util.Properties;

/**
 *
 * @author mmuesly
 */
public class TestHelper {
  
  public static void setupSolver(){
    //Setup Z3 solver, as wee need it during enrolling of Transition Systems.
    Properties conf = new Properties();
    conf.setProperty("symbolic.dp", "Z3");
    conf.setProperty("symbolic.dp.z3.bitvectors", "false");
    conf.setProperty("log.finest", "psyco");
    ConstraintSolver solver = ConstraintSolverFactory.createSolver(conf);
    SolverInstance.getInstance().setSolver(solver);
  }
}
