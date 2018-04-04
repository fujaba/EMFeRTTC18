package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.jastadd.model.Root;

public interface BenchmarkableSolver extends Solver {

  /**
   * @return a descriptive, unique, short name, e.g., "ilp"
   */
  String getName();

  /**
   * @return whether this solver generates an intermediate model. Defaults to <code>false</code>.
   * Solvers returning <code>true</code> here, should also override {@link #getLastGenerationTime()}.
   */
  default boolean doesGeneration() {
    return false;
  }

  /**
   * Return the generation time in milliseconds for the last finished call of {@link #solve(Root)}.
   * Defaults to returning zero for all solvers that do not generate.
   * Ignored, if {@link #doesGeneration()} is <code>false</code>.
   * @return generation time in milliseconds
   */
  default long getLastGenerationTime() {
    return 0;
  }

  /**
   * @return solving time in milliseconds for the last finished call of {@link #solve(Root)}.
   */
  long getLastSolvingTime();

  /**
   * @return objective value for the last finished call of {@link #solve(Root)}.
   */
  double getLastObjective();

  /**
   * @return whether this solver reached the timeout for the last finished call of {@link #solve(Root)}.
   */
  boolean hadTimeout();

}
