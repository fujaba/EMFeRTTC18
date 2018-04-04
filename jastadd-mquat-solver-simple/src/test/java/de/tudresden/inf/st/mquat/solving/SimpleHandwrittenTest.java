package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.solving.simple.SimpleSolver;

public class SimpleHandwrittenTest extends HandwrittenTestSuite {

  @Override
  protected Solver getSolver() {
    return new SimpleSolver(10000);
  }
}
