package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.data.TestGeneratorSettings;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import de.tudresden.inf.st.mquat.utils.TestUtils;
import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import de.tudresden.inf.st.mquat.solving.ilp.ILPDirectSolver;
import de.tudresden.inf.st.mquat.solving.ilp.ILPExternalSolver;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import de.tudresden.inf.st.mquat.utils.TestGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(Parameterized.class)
public class ILPSolveTest {

  private static Logger logger;
  private static TestGeneratorSettings settings = new TestGeneratorSettings() {{
    minTopLevelComponents = 1;
    maxTopLevelComponents = 3;

    minAvgNumImplSubComponents = 0;
    maxAvgNumImplSubComponents = 2;

    minImplSubComponentDerivation = 0;
    maxImplSubComponentDerivation = 1;

    minAvgNumCompSubComponents = 0;
    maxAvgNumCompSubComponents = 2;

    minCompSubComponentDerivation = 0;
    maxCompSubComponentDerivation = 1;

    minComponentDepth = 1;
    maxComponentDepth = 3;

    minNumImplementations = 1;
    maxNumImplementations = 2;

    minResourceRatio = 1d;
    maxResourceRatio = 2d;
    stepResourceRatio = .1d;

    minRequests = 0;
    maxRequests = 100;
    stepRequests = 25;

    minCpus = 1;
    maxCpus = 3;

    seed = 0;
    verbose = false;
    shouldExitOnWarnings = true;
  }};
  private static Integer[] testIdsToSkip = {
      298,
      343, 352, 355, 357, 358,
      477, 478, 479,
      519, 520, 521, 522, 523, 524, 531, 532, 533, 534, 535, 536, 537, 538};
  private static Set<Integer> testIdsToSkipAsSet;
  private static final int startingTestId = 506;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    StopWatch watch = StopWatch.start();
    List<Object[]> result = new ArrayList<>();
    TestGenerator generator = new TestGenerator(settings);
    System.out.println("*** Generating test generator");
    generator.generateScenarioGenerator((gen, testId) -> {
      if (testId > 150) { return false; } // skip other test-cases for now
//      if (testId < startingTestId) { return false; }
      Root model = gen.generate();
      String name = testId + model.description();
      result.add(new Object[]{name, testId, model, gen});
      return true;
    });
    long diff = watch.time(TimeUnit.MILLISECONDS);
    System.out.println("Generation took " + diff + "ms.");
    return result;
  }

  @BeforeClass
  public static void initLogger() {
    Assume.assumeTrue(TestUtils.shouldTestLongRunning());
    logger = LogManager.getLogger(ILPSolveTest.class);
    testIdsToSkipAsSet = new HashSet<>(Arrays.asList(testIdsToSkip));
  }

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @Before
  public void setup() {
    System.gc();
  }

  private Solver externalSolver() {
    // set to false to analyse created temporary files
    return new ILPExternalSolver().setDeleteFilesOnExit(true).setTimeout(10, TimeUnit.SECONDS);
  }

  private Solver directSolver() {
    // set to true to analyse created temporary files
    return new ILPDirectSolver().setWriteFiles(false).setTimeout(10, TimeUnit.SECONDS);
  }

  private String name;
  private int testId;
  private Root model;
  private ScenarioGenerator gen;

  public ILPSolveTest(String name, int testId, Root model, ScenarioGenerator gen) {
    this.name = name;
    this.testId = testId;
    this.model = model;
    this.gen = gen;
  }

  @Test
  public void testWithExternal() {
    testWith(externalSolver());
  }

  @Test
  public void testWithDirect() {
    testWith(directSolver());
  }

  private void testWith(Solver solver) {
    Assume.assumeTrue(TestUtils.shouldTestLongRunning());
    Assume.assumeFalse("Skipping complicated test case " + testId, testIdsToSkipAsSet.contains(testId));
    System.out.println("name=" + name);
    Solution solution;
    try {
      solution = solver.solve(model);
    } catch (SolvingException e) {
      collector.addError(e);
      return;
    }
    logger.debug("Start validation");
    collector.checkThat("Test" + name + " failed", true, equalTo(solution.isValid()));
    logger.debug("End validation, begin compute objective");
    double actualObjective = solution.computeObjective();
    double initialObjective = gen.getInitialSolution().computeObjective();
    logger.debug("End compute objective");
    if (actualObjective != initialObjective) {
      logger.info("Different objective: {}. Initial was {}", actualObjective, initialObjective);
    }
  }
}
