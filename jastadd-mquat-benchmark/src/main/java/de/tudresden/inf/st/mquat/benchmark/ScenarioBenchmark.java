package de.tudresden.inf.st.mquat.benchmark;

import de.tudresden.inf.st.mquat.benchmark.data.BenchmarkSettings;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import de.tudresden.inf.st.mquat.solving.BenchmarkableSolver;
import de.tudresden.inf.st.mquat.utils.TestGenerator;

import java.io.BufferedWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark running a predefined scenario.
 *
 * @author rschoene - Initial contribution
 */
public class ScenarioBenchmark extends Benchmark {

  private int repetitions;

  public ScenarioBenchmark(BenchmarkSettings settings, List<BenchmarkableSolver> solvers, int repetitions) {
    super();
    this.repetitions = repetitions;
    this.settings = settings;
    setResultFilePattern(settings.resultFilePattern, true);
    for (BenchmarkableSolver solver : solvers) {
      addSolver(solver);
    }
  }

  @Override
  protected String getDirectory() {
    return "scenarios";
  }

  @Override
  protected boolean runScenario(ScenarioGenerator gen, int testId, BufferedWriter writer, Path path, ModelFilePattern mfp, SolutionFilePattern sfp, AtomicInteger failCount, AtomicInteger totalCount) {
    for (int i = 0; i < this.repetitions; i++) {
      if (!super.runScenario(gen, testId, writer, path, mfp, sfp, failCount, totalCount)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void describe(StringBuilder sb, TestGenerator testGen) {
    super.describe(sb, testGen);
    append(sb, "Repetitions", this.repetitions);
    sb.append("Result file: ").append(this.settings.resultFilePattern);
  }
}
