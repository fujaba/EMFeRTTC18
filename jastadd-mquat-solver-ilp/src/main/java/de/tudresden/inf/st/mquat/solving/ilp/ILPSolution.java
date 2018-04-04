package de.tudresden.inf.st.mquat.solving.ilp;

import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;

import java.util.Collections;

public class ILPSolution extends Solution {
  private double objective;

  public ILPSolution(Root model) {
    setModel(model);
  }

  public double getObjective() {
    return objective;
  }

  public void setObjective(double objective) {
    this.objective = objective;
  }
}
