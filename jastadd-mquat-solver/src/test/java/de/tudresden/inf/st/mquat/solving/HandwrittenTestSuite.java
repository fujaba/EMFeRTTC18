package de.tudresden.inf.st.mquat.solving;

import beaver.Parser;
import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.utils.ParserUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.rules.ErrorCollector;

import java.io.*;
import java.net.URL;
import java.util.Iterator;

import static org.hamcrest.core.IsEqual.equalTo;

public abstract class HandwrittenTestSuite {
  private static Logger logger;
  private Solver solver;

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @BeforeClass
  public static void setupClass() {
    logger = LogManager.getLogger(HandwrittenTestSuite.class);
  }

  @Before
  public void setupSolverForTest() {
    this.solver = getSolver();
  }

  /**
   * Create and return the solver to use in this test suite
   * @return a solver instance
   */
  protected abstract Solver getSolver();

  private Tuple<Root, Solution> loadAndSolve(String filename) throws IOException, Parser.Exception, SolvingException {
    Root model = ParserUtils.load(filename, HandwrittenTestSuite.class);
    Solution solution = solver.solve(model);
    if (logger.isDebugEnabled()) {
      MquatString out = solution.print(new MquatWriteSettings(" "));
      logger.debug("Solution:\n{}", out);
    }
    return new Tuple<>(model, solution);
  }

  private Assignment assertAssignment(Tuple<Root, Solution> modelAndSolution,
                                      int request, String impl, String resource) {
    Root model = modelAndSolution.getFirstElement();
    Assignment expectedAssignment = new Assignment();
    expectedAssignment.setRequest(model.getRequest(request));
    expectedAssignment.setImplementation(model.findImplementationByName(impl));
    expectedAssignment.setResourceMapping(new ResourceMapping(expectedAssignment.getImplementation().getResourceRequirement().getInstance(0), model.findResourceByName(resource), new List<>()));
    // check if assignment matches (partly) one listed in the solution
    Iterator<Assignment> assignmentIterator = modelAndSolution.getSecondElement().assignmentIterator();
    while (assignmentIterator.hasNext()) {
      Assignment actualAssignment = assignmentIterator.next();
      if (matches(actualAssignment, expectedAssignment)) {
        Assert.assertNotNull(actualAssignment);
        return actualAssignment;
      }
    }
    Assert.fail(String.format("Did not find match of assignment: %s on %s for request%s",
        impl, resource, request));
    throw new AssertionError();
  }

  private void assertComponentRequirement(Assignment requiringAssignment,
                                          String instanceName, Assignment expectedProvidingAssignment) {
    Instance instance = requiringAssignment.getImplementation().findInstanceByName(instanceName);
    Assignment actualProvidingAssignment = requiringAssignment.mappedAssignment(instance);
    Assert.assertEquals(String.format("Not matching assignment for %s", instanceName),
        expectedProvidingAssignment, actualProvidingAssignment);
  }

  /**
   * Check if request, configuration and resource of the given assignments are equal
   * @param actualAssignment   assignment in the computed solution
   * @param expectedAssignment expected assignment defined in the test case
   * @return <code>true</code> if both match, <code>false</code> otherwise
   */
  private boolean matches(Assignment actualAssignment, Assignment expectedAssignment) {
    return actualAssignment.getRequest().equals(expectedAssignment.getRequest()) &&
        actualAssignment.getImplementation().equals(expectedAssignment.getImplementation()) &&
        actualAssignment.getResource().equals(expectedAssignment.getResource());
  }

  private void assertValidSolution(Tuple<Root, Solution> modelAndSolution) {
//    Assert.assertTrue("Solution is not valid", modelAndSolution.getSecondElement().isValid());
    collector.checkThat("Solution is not valid", true,
        equalTo(modelAndSolution.getSecondElement().isValid()));
  }

  @Test
  public void test_01() throws IOException, Parser.Exception, SolvingException {
    Tuple<Root, Solution> modelAndSolution = loadAndSolve("test_01.txt");
    Assignment config_0i0m0 = assertAssignment(modelAndSolution, 0, "config_0i0m0", "r0");
    Assignment config_1i0m0 = assertAssignment(modelAndSolution, 0, "config_1i0m0", "r1");
    assertComponentRequirement(config_0i0m0,"other", config_1i0m0);
    assertValidSolution(modelAndSolution);
  }

  @Test
  public void test_02() throws IOException, Parser.Exception, SolvingException {
    Tuple<Root, Solution> modelAndSolution = loadAndSolve("test_02.txt");
    Assignment config_0i0m0 = assertAssignment(modelAndSolution, 0, "config_0i0m0", "r0");
    Assignment config_1i0m0 = assertAssignment(modelAndSolution, 0, "config_1i0m0", "r1");
    assertComponentRequirement(config_0i0m0,"other", config_1i0m0);
    assertValidSolution(modelAndSolution);
  }

  @Test
  public void test_03() throws IOException, Parser.Exception, SolvingException {
    Tuple<Root, Solution> modelAndSolution = loadAndSolve("test_03.txt");
    assertValidSolution(modelAndSolution);
    Assignment r0config_0i0m0 = assertAssignment(modelAndSolution, 0, "config_0i0m0", "r0");
    Assignment r0config_1i0m0 = assertAssignment(modelAndSolution, 0, "config_1i0m0", "r1");
    assertAssignment(modelAndSolution, 1, "config_1i0m0", "r2");
    assertComponentRequirement(r0config_0i0m0,"other", r0config_1i0m0);
  }

  @Test
  public void test_04() throws IOException, Parser.Exception, SolvingException {
    Tuple<Root, Solution> modelAndSolution = loadAndSolve("test_04.txt");
    assertValidSolution(modelAndSolution);
    Assignment config_0i0m0 = assertAssignment(modelAndSolution, 0, "config_0i0m0", "r0");
    Assignment config_1i0m0 = assertAssignment(modelAndSolution, 0, "config_1i0m0", "r1");
    Assignment config_2i0m0 = assertAssignment(modelAndSolution, 0, "config_2i0m0", "r2");
    assertComponentRequirement(config_0i0m0,"alpha", config_1i0m0);
    assertComponentRequirement(config_0i0m0,"beta", config_2i0m0);
  }

  @Test
  public void test_05() throws IOException, Parser.Exception, SolvingException {
    Tuple<Root, Solution> modelAndSolution = loadAndSolve("test_05.txt");
    assertValidSolution(modelAndSolution);
    Assignment configA = assertAssignment(modelAndSolution, 0, "configA0", "r0");
    Assignment configB = assertAssignment(modelAndSolution, 0, "configB0", "r1");
    Assignment configC = assertAssignment(modelAndSolution, 0, "configC0", "r4");
    Assignment configD = assertAssignment(modelAndSolution, 0, "configD0", "r3");
    Assignment configE = assertAssignment(modelAndSolution, 0, "configE0", "r2");
    Assignment configF = assertAssignment(modelAndSolution, 0, "configF0", "r5");
    Assignment configG = assertAssignment(modelAndSolution, 0, "configG0", "r6");
    assertComponentRequirement(configA,"beta", configB);
    assertComponentRequirement(configA,"epsilon", configE);
    assertComponentRequirement(configB,"chi", configC);
    assertComponentRequirement(configB,"delta", configD);
    assertComponentRequirement(configE,"phi", configF);
    assertComponentRequirement(configE,"gamma", configG);
  }

}
