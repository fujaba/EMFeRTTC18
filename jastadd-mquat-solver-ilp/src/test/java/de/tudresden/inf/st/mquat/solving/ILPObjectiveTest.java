package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.generator.ScenarioDescription;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import de.tudresden.inf.st.mquat.solving.ilp.ILPExternalSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ILPObjectiveTest {

  private static Logger logger;

  @BeforeClass
  public static void initLogger() {
    logger = LogManager.getLogger(ILPObjectiveTest.class);
  }

  @Test
  public void test_config_01() throws SolvingException {
    int tlc = 1;
    int iac = 1;
    int isd = 0;
    int cac = 0;
    int csd = 0;
    int dep = 2;
    int imp = 2;
    int res = 10;
    int req = 1;
    int cpu = 1;
    int seed = 0;

    ScenarioGenerator generator = new ScenarioGenerator(new ScenarioDescription(tlc, iac, isd, cac, csd, dep, imp, res, req, cpu, seed));
    Root model = generator.generate();
    ILPExternalSolver solver = new ILPExternalSolver().setDeleteFilesOnExit(false);
    Solution solution = solver.solve(model);
    Assert.assertTrue(solution.isValid());
    logger.info("Solution (objective={}): {}", solution.computeObjective(), solution);
  }
}
