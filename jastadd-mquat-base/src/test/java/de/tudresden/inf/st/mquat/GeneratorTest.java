package de.tudresden.inf.st.mquat;

import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import de.tudresden.inf.st.mquat.utils.TestGenerator;
import de.tudresden.inf.st.mquat.utils.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;

public class GeneratorTest {

  private static Logger logger;

  @BeforeClass
  public static void initLogger() {
    Assume.assumeTrue(TestUtils.shouldTestLongRunning());
    logger = LogManager.getLogger(GeneratorTest.class);
  }

  @Test
  public void testGenerator() {
    long startTime = System.nanoTime();

    TestGenerator testGenerator = new TestGenerator();
    int total = testGenerator.generateScenarioGenerator((generator, testId) -> {
      Root model = generator.generate();
      Solution solution = generator.getInitialSolution();
      logger.debug(testId + " " + model.description() + " has " + solution.allAssignments().size() + " assignments.");
      Assert.assertTrue(solution.isValid());
      return true;
    });

    long endTime = System.nanoTime();
    double totalTimeInSec = (endTime - startTime) / 1000000000d;
    double testsPerSec = (total + 1) / totalTimeInSec;
    logger.info("Testing speed was " + testsPerSec + " tests/sec.");
  }

}
