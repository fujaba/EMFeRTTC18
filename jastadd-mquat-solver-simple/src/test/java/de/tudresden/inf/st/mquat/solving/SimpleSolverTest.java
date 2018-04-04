package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.generator.ScenarioDescription;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import de.tudresden.inf.st.mquat.solving.simple.SimpleSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleSolverTest {

  private static Logger logger;

  @BeforeClass
  public static void initLogger() {
    logger = LogManager.getLogger(SimpleSolverTest.class);
  }

  /**
   * tests the simple solver with one very simple use case
   */
  @Test
  public void testSimpleSolver() throws SolvingException {
    int tlc = 1;
    int iac = 2;
    int isd = 0;
    int cac = 0;
    int csd = 0;
    int dep = 2;
    int imp = 1;
    int mod = 3;
    double res = 1.5d;
    int nfp = 0;
    int req = 3;
    int cpu = 1;
    int seed = 0;

    ScenarioGenerator generator = new ScenarioGenerator(new ScenarioDescription(tlc, iac, isd, cac, csd, dep, imp, res, req, cpu, seed));

    Root model = generator.generate();
    SimpleSolver solver = new SimpleSolver(20000);

    Solution solution = solver.solve(model);

    Assert.assertNotNull(solution);

    logger.info("the best solution is {} and has an objective of {}.", (solution.isValid() ? "valid" : "invalid"), solution.computeObjective());

  }
}
