package de.tudresden.inf.st.mquat.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tudresden.inf.st.mquat.data.TestGeneratorSettings;
import de.tudresden.inf.st.mquat.benchmark.data.BenchmarkSettings;
import de.tudresden.inf.st.mquat.benchmark.data.ScenarioData;
import de.tudresden.inf.st.mquat.benchmark.data.ScenarioSettings;
import de.tudresden.inf.st.mquat.solving.BenchmarkableSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Runs all defined scenarios using every solver defined.
 *
 * @author rschoene - Initial contribution
 */
public class FullBenchmarkMain {

  private static Logger logger = LogManager.getLogger(FullBenchmarkMain.class);

  public static void main(String[] args) {
    List<Benchmark> benchmarks = createFromConfig(args);
    if (benchmarks == null || benchmarks.isEmpty()) {
      logger.fatal("Could not create benchmarks. Exiting now.");
      return;
    }
    benchmarks.forEach(Benchmark::run);
  }

  private static List<Benchmark> createFromConfig(String[] args) {
    Logger logger = LogManager.getLogger(CustomBenchmarkMain.class);
    ObjectMapper mapper = Utils.getMapper();
    ScenarioSettings settings;
    try {
      settings = Utils.readFromResource(mapper, "scenarios.json", ScenarioSettings.class);
    } catch (IOException e) {
      logger.catching(e);
      return null;
    }
    final List<Integer> allowedIds = Arrays.stream(args)
        .map(FullBenchmarkMain::parseInt)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    final List<String> allowedNames = Arrays.asList(args);
    final boolean takeAll = args.length == 0;
    final List<BenchmarkableSolver> solvers = settings.solvers.stream()
        .map(SolverFactory::getSolverByName).collect(Collectors.toList());
    return settings.scenarios.stream()
        .filter(data -> takeAll || allowedIds.contains(data.getId()) || allowedNames.contains(data.name))
        .map(data -> new ScenarioBenchmark(from(settings, data), solvers, settings.repetitions))
        .collect(Collectors.toList());
  }

  private static Integer parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static BenchmarkSettings from(ScenarioSettings settings, ScenarioData data) {
    BenchmarkSettings result = new BenchmarkSettings();
    result.kind = "normal";
    String scenarioName = data.getId() + "_" + data.name;
    result.resultFilePattern = scenarioName + ".csv";
    result.modelFilePattern = scenarioName + ".txt";
    result.solutionFilePattern = scenarioName + "-%(solver)s.txt";
    result.logLevel = settings.logLevel;
    result.path = settings.path;
    result.solvers = settings.solvers;
    TestGeneratorSettings tgs = new TestGeneratorSettings();
    tgs.minTopLevelComponents = tgs.maxTopLevelComponents = 1;
    tgs.minAvgNumImplSubComponents = tgs.maxAvgNumImplSubComponents = 0;
    tgs.minImplSubComponentDerivation = tgs.maxImplSubComponentDerivation = 0;
    tgs.minAvgNumCompSubComponents = tgs.maxAvgNumCompSubComponents = 2;
    tgs.minCompSubComponentDerivation = tgs.maxCompSubComponentDerivation = 0;
    tgs.minComponentDepth = data.depth;
    tgs.maxComponentDepth = data.depth;
    tgs.minNumImplementations = data.variants;
    tgs.maxNumImplementations = data.variants;
    tgs.minRequests = data.requests;
    tgs.maxRequests = data.requests;
    tgs.stepRequests = 1;
    tgs.minCpus = tgs.maxCpus = 1;
    tgs.minResourceRatio = data.resources;
    tgs.maxResourceRatio = data.resources;
    tgs.stepResourceRatio = 1.0;
    tgs.timeoutValue = settings.timeoutValue;
    tgs.timeoutUnit = settings.timeoutUnit;
    tgs.seed = settings.seed;
    tgs.total = settings.repetitions;
    tgs.verbose = true;
    result.updateBasic(tgs);
    return result;
  }

}
