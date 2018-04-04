package de.tudresden.inf.st.mquat.solving.ilp;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.solving.BenchmarkableSolver;
import de.tudresden.inf.st.mquat.solving.SolverUtils;
import de.tudresden.inf.st.mquat.solving.SolvingException;
import de.tudresden.inf.st.mquat.utils.StaticSettings;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractILPSolver implements BenchmarkableSolver {

  protected transient final Logger logger;
  protected long lastGeneration;
  protected long lastSolving;
  protected long lastSolutionCreation;
  protected double lastObjective;
  protected transient long timeoutValue;
  protected transient TimeUnit timeoutUnit;
  protected transient long timeoutValueOriginal;
  protected transient TimeUnit timeoutUnitOriginal;
  protected long timeoutInSeconds;
  protected boolean timedOut;
  private boolean resetTimeOut;

  /**
   * Create a new, abstract solver with default settings.
   * Defaults are:
   * <ul>
   *   <li>1 minute timeout</li>
   * </ul>
   * @param logger the logger to use
   * @see ILPDirectSolver#setTimeout(long, TimeUnit)
   */
  public AbstractILPSolver(Logger logger) {
    this.logger = logger;
    this.resetTimeOut = false;
    setTimeout(1, TimeUnit.MINUTES);
    reset();
  }

  protected void cleanup(StopWatch watch) {
    setTimeout(this.timeoutValueOriginal, this.timeoutUnitOriginal);
    lastSolving = watch.time(TimeUnit.MILLISECONDS);
    logger.debug("Solving took " + lastSolving + "ms.");
  }

  /**
   * Reset times and the objective (i.e., all member fields beginning with "last").
   */
  protected void reset() {
    this.lastGeneration = 0;
    this.lastSolving = 0;
    this.lastSolutionCreation = 0;
    this.lastObjective = 0;
    this.timedOut = false;
  }

  @Override
  public synchronized Solution solve(Root model) throws SolvingException {
    reset();
    if (model.getNumRequest() == 0) {
      return Solution.emptySolutionOf(model);
    }

    StopWatch watch = StopWatch.start();
    final ILP ilp = model.getILP();
    lastGeneration = watch.time(TimeUnit.MILLISECONDS);
    logger.debug("ILP-Generation took {}ms.", lastGeneration);
    if (ilp.hasTimeout()) {
      logger.error("ILP-Generation exceeded timeout, message: '{}'", ilp.timeoutReason());
      return Solution.emptySolutionOf(model);
    }

    if (ilp.getNumIlpVariable() != ilp.getNumIlpBound()) {
      logger.warn("Different variable ({}) and bound ({}) count", ilp.getNumIlpVariable(), ilp.getNumIlpBound());
    }

    // temporary update timeout to the remaining time.
    // calling cleanup will reset it to the original value
    this.timeoutValueOriginal = this.timeoutValue;
    this.timeoutUnitOriginal = this.timeoutUnit;
    long nanosRemaining = this.timeoutUnit.toNanos(this.timeoutValue) - watch.time();
    if (nanosRemaining < 0) {
      logger.error("ILP-Generation actually timed out");
      cleanup(watch);
      return Solution.emptySolutionOf(model);
    }
    setTimeout(nanosRemaining, TimeUnit.NANOSECONDS);

    List<IlpVariable> variablesSetToOne = new ArrayList<>();
    watch.reset();

    // call to abstract method
    lastObjective = solve0(model, watch, variablesSetToOne);

    cleanup(watch);
    return populateSolution(variablesSetToOne, new ILPSolution(model));
  }

  /**
   * Solves the model. The method <code>model.getILP()</code> was already called and can be assumed to be cached.
   * @param model             the model to solve
   * @param watch             a stop watch to be passed to cleanup if necessary
   * @param variablesSetToOne the means of a solution, i.e., which variables are set to one
   * @return the objective value
   * @throws SolvingException if anything went wrong
   */
  protected abstract double solve0(Root model, StopWatch watch, List<IlpVariable> variablesSetToOne) throws SolvingException;

  protected ILPSolution populateSolution(List<IlpVariable> variablesSetToOne, ILPSolution result) throws SolvingException {
    List<Assignment> listOfAssignments = new ArrayList<>();
    for (IlpVariable var : variablesSetToOne) {
      logger.debug("Found, that {} = 1", var.getName());
      if (var.isMappingVariable()) {
        IlpMappingVariable mappingVar = var.asMappingVariable();
        Assignment assignment = new Assignment();
        assignment.setRequest(mappingVar.getRequest());
        assignment.setImplementation(mappingVar.getImpl());
        assignment.setResourceMapping(new ResourceMapping(assignment.getImplementation().getResourceRequirement().getInstance(0), mappingVar.getResource(), new de.tudresden.inf.st.mquat.jastadd.model.List<>()));
        listOfAssignments.add(assignment);
      }
    }
    lastSolutionCreation = SolverUtils.populateSolution(listOfAssignments, result, logger);
    return result;
  }

  public AbstractILPSolver setTimeout(long timeoutValue, TimeUnit timeoutUnit) {
    this.timeoutUnit = timeoutUnit;
    this.timeoutValue = timeoutValue;
    StaticSettings.put(Root.ILP_TIMEOUT_VALUE, timeoutValue);
    StaticSettings.put(Root.ILP_TIMEOUT_UNIT, timeoutUnit);
    recomputeTimeoutInSeconds();
    return this;
  }

  protected void recomputeTimeoutInSeconds() {
    this.timeoutInSeconds = timeoutUnit.toSeconds(timeoutValue);
  }

  @Override
  public boolean doesGeneration() {
    return true;
  }

  @Override
  public long getLastGenerationTime() {
    return lastGeneration;
  }

  @Override
  public long getLastSolvingTime() {
    return lastSolving + lastSolutionCreation;
  }

  @Override
  public double getLastObjective() {
    return lastObjective;
  }

  @Override
  public boolean hadTimeout() {
    return this.timedOut;
  }
}
