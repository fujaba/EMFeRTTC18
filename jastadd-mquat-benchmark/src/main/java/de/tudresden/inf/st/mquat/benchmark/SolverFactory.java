package de.tudresden.inf.st.mquat.benchmark;

import de.tudresden.inf.st.mquat.solving.BenchmarkableSolver;
import de.tudresden.inf.st.mquat.solving.ilp.ILPDirectSolver;
import de.tudresden.inf.st.mquat.solving.ilp.ILPExternalSolver;
import de.tudresden.inf.st.mquat.solving.simple.SimpleSolver;
import uniks.EMFeRSolver;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gathering point for all solvers.
 *
 * @author rschoene - Initial contribution
 */
public class SolverFactory {

  private static Map<String, BenchmarkableSolver> availableSolvers;

  private static Map<String, BenchmarkableSolver> createAvailableSolversIfNeeded() {
    if (availableSolvers == null) {
      availableSolvers = Stream.of(
              new EMFeRSolver(),
              // new ILPExternalSolver(),
              new ILPDirectSolver(),
              new SimpleSolver()
      ).collect(Collectors.toMap(BenchmarkableSolver::getName, Function.identity()));
    }
    return availableSolvers;
  }

  /**
   * Get a solver by its name. Returns <code>null</code> if no solver exists with this name.
   * @param name the name of the solver to search for
   * @return an instance of the solver, or <code>null</code>
   */
  public static BenchmarkableSolver getSolverByName(String name) {
    return createAvailableSolversIfNeeded().get(name);
  }

}
