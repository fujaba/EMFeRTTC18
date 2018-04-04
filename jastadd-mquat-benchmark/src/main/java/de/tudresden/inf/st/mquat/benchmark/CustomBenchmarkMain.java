package de.tudresden.inf.st.mquat.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tudresden.inf.st.mquat.benchmark.data.BenchmarkSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class CustomBenchmarkMain {

  private static Benchmark createFromConfig() {
    Logger logger = LogManager.getLogger(CustomBenchmarkMain.class);
    BenchmarkSettings settings, localSettings;
    ObjectMapper mapper = Utils.getMapper();
    try {
      settings = Utils.readFromResource(mapper, "benchmark-settings.json", BenchmarkSettings.class);
    } catch (IOException e) {
      logger.catching(e);
      throw new RuntimeException("Could not read settings! Exiting.", e);
    }
    try {
      localSettings = Utils.readFromResource(mapper, "local-benchmark-settings.json", BenchmarkSettings.class);
    } catch (IOException ignored) {
      // use an empty local settings, no value will be changed
      LogManager.getLogger(CustomBenchmarkMain.class).info("No local settings found, using default values.");
      localSettings = new BenchmarkSettings();
    }
    settings.update(localSettings);
    Benchmark result;
    switch (settings.kind) {
      case "normal": result = new Benchmark(); break;
      default: throw new RuntimeException("Unknown benchmark kind: " + settings.kind);
    }
    result.setSettings(settings);
    settings.solvers.forEach(solverName -> result.addSolver(SolverFactory.getSolverByName(solverName)));
    return result;
  }

  public static void main(String[] args) {
    Benchmark benchmark = createFromConfig();
    benchmark.run();
  }
}
