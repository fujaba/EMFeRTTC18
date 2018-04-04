package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.solving.ilp.ILPExternalSolver;

public class ILPExternalHandwrittenTest extends HandwrittenTestSuite {
  @Override
  protected Solver getSolver() {
    // set to false for debugging
    return new ILPExternalSolver().setDeleteFilesOnExit(true);
  }
}
