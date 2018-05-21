package de.tudresden.inf.st.mquat.solving;

import uniks.EMFeRSolver;

public class EMFeRHandwrittenTest extends HandwrittenTestSuite {

  @Override
  protected Solver getSolver() {
    return new EMFeRSolver(10000);
  }
}
