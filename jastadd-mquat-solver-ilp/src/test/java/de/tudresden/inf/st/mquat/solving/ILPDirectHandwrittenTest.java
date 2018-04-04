package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.solving.ilp.ILPDirectSolver;

public class ILPDirectHandwrittenTest extends HandwrittenTestSuite {
  @Override
  protected Solver getSolver() {
    // set to true for debugging
    return new ILPDirectSolver().setWriteFiles(false);
  }
}
