package de.tudresden.inf.st.mquat.utils;

import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;

public interface ScenarioGeneratorConsumer {
  /**
   * Do something with the scenario generator. Return whether to move on with further generation.
   * @param gen    the generator to work with
   * @param testId the current number of test
   * @return <code>false</code> to stop generation, <code>true</code> to move on
   */
  boolean run(ScenarioGenerator gen, int testId);
}
