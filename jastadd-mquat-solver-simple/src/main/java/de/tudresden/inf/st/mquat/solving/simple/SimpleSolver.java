package de.tudresden.inf.st.mquat.solving.simple;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.solving.BenchmarkableSolver;
import de.tudresden.inf.st.mquat.solving.Solver;
import de.tudresden.inf.st.mquat.solving.SolverUtils;
import de.tudresden.inf.st.mquat.solving.SolvingException;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleSolver implements BenchmarkableSolver {

  private static final Logger logger = LogManager.getLogger(SimpleSolver.class);

  private Solution lastSolution;
  private long lastSolvingTime;

  private int solutionCounter;

  private StopWatch stopWatch;

  private long maxSolvingTime;
  private boolean timedOut;

  public SimpleSolver() {
    this(Long.MAX_VALUE);
  }

  public SimpleSolver(long maxSolvingTime) {
    this.maxSolvingTime = maxSolvingTime;
    reset();
  }

  private static void assignResource(Assignment assignment, Resource resource) {
    Implementation impl = assignment.getImplementation();

    ResourceMapping mapping = new ResourceMapping(impl.getResourceRequirement().getInstance(0), resource, new de.tudresden.inf.st.mquat.jastadd.model.List<>());
    SolverUtils.populateResourceMapping(mapping, impl.getResourceRequirement(), resource);
    assignment.setResourceMapping(mapping);
  }

  private int checkAssignment(Solution solution, List<Solution> solutions, List<Assignment> assignments, List<Set<Resource>> possibleResources, int index, Stack<Resource> usedResources) {
    int checkCounter = 0;

    Assignment assignment = assignments.get(index);
    for (Resource resource : possibleResources.get(index)) {

      if (stopWatch.time(TimeUnit.MILLISECONDS) > maxSolvingTime) {
        return checkCounter;
      }

      if (usedResources.contains(resource)) continue;
      assignResource(assignment, resource);
      usedResources.push(resource);
      checkCounter++;
      if (index == assignments.size() - 1) {
        if (solution.isValid()) {
          solutionCounter++;
          if (solutions.isEmpty() || solution.computeObjective() < solutions.get(solutions.size() - 1).computeObjective()) {
            Solution clone = solution.deepCopy();
            solutions.add(clone);
            logger.info("found a better solution with an objective of {}.", solution.computeObjective());
          }

        }
      } else {
        checkCounter += checkAssignment(solution, solutions, assignments, possibleResources, index + 1, usedResources);
      }
      usedResources.pop();
    }
    return checkCounter;
  }

  @Override
  public Solution solve(Root model) throws SolvingException {
    reset();
    if (model.getNumRequest() == 0) {
      return Solution.emptySolutionOf(model);
    }
    int numAssignments = 0;
    int numSoftwareSolutions = 0;
    int numTotalSoftwareSolutions = 0;

    stopWatch = StopWatch.start();

    List<Solution> solutions = new ArrayList<>();

    // iterate all possible assignments
    // Note, that this only considers assignments of one configuration to each hardware component

    Solution currentSolution = Solution.createSoftwareSolution(model);
//    currentSolution.trace().process(new LoggerProcessor());

    de.tudresden.inf.st.mquat.jastadd.model.List<Resource> resources = model.getHardwareModel().getResources();

    boolean hasNextSoftwareAssignment;
    do {

      numTotalSoftwareSolutions++;

      if (currentSolution.isSoftwareValid()) {

        numSoftwareSolutions++;

        List<Assignment> assignments = currentSolution.allAssignments();

        // initialize the lists of possible assignments
        List<Set<Resource>> possibleResources = new ArrayList<>(assignments.size());

        for (Assignment assignment : assignments) {
          Set<Resource> resourceList = new HashSet<>();
          for (Resource resource : resources) {
            assignResource(assignment, resource);
            if (assignment.isValid()) {
              resourceList.add(resource);
            }
          }
          possibleResources.add(resourceList);
        }

        numAssignments += checkAssignment(currentSolution, solutions, assignments, possibleResources, 0, new Stack<>());
      }

      if (stopWatch.time(TimeUnit.MILLISECONDS) > maxSolvingTime) {
        this.timedOut = true;
        logger.warn("Timeout! Solving terminated!");
        break;
      }

      hasNextSoftwareAssignment = currentSolution.nextSoftwareAssignment();
    } while (hasNextSoftwareAssignment);

    logger.info("Number of total software solutions: {}", numTotalSoftwareSolutions);
    logger.info("Number of iterated software solutions: {}", numSoftwareSolutions);
    logger.info("Number of iterated solutions: {}", numAssignments);
    logger.info("Number of correct solutions: {}", solutionCounter);

    if (solutions.size() > 0) {
      lastSolution = solutions.get(solutions.size() - 1);
    } else {
      lastSolution = Solution.emptySolutionOf(model);
      logger.warn("Found no solution!");
    }

    lastSolvingTime = stopWatch.time(TimeUnit.MILLISECONDS);

    return lastSolution;
  }

  private void reset() {
    this.lastSolution = null;
    this.solutionCounter = 0;
    this.lastSolvingTime = 0;
    this.timedOut = false;
  }

  @Override
  public String getName() {
    return "simple";
  }

  @Override
  public long getLastSolvingTime() {
    return lastSolvingTime;
  }

  @Override
  public double getLastObjective() {
    if (lastSolution != null) {
      return lastSolution.computeObjective();
    } else {
      // TODO throw exception or do something reasonable
      return 0d;
    }
  }

  @Override
  public Solver setTimeout(long timeoutValue, TimeUnit timeoutUnit) {
    this.maxSolvingTime = timeoutUnit.toMillis(timeoutValue);
    return this;
  }

  @Override
  public boolean hadTimeout() {
    return this.timedOut;
  }
}
