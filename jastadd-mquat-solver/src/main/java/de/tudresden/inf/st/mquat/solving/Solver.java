package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;

import java.util.concurrent.TimeUnit;

public interface Solver {

  /**
   * Solve the given model.
   * @param model the model to solve
   * @return a solution w.r.t. the model
   * @throws SolvingException if something went wrong
   */
  Solution solve(Root model) throws SolvingException;

  /**
   * Set the maximum amount of time for calls to {@link Solver#solve(Root)}.
   * Defaults to ignoring the specified timeout.
   * @param timeoutValue value for the timeout
   * @param timeoutUnit  used unit for the timeout
   * @return this
   */
  default Solver setTimeout(long timeoutValue, TimeUnit timeoutUnit) {
    return this;
  }

}
