package de.tudresden.inf.st.mquat.benchmark;

import de.tudresden.inf.st.mquat.data.TestGeneratorSettings;
import de.tudresden.inf.st.mquat.benchmark.data.BenchmarkSettings;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import de.tudresden.inf.st.mquat.jastadd.model.MquatString;
import de.tudresden.inf.st.mquat.jastadd.model.MquatWriteSettings;
import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import de.tudresden.inf.st.mquat.solving.BenchmarkableSolver;
import de.tudresden.inf.st.mquat.solving.SolvingException;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import de.tudresden.inf.st.mquat.utils.TestGenerator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static de.tudresden.inf.st.mquat.utils.MapCreator.e;
import static java.nio.file.Files.exists;

@SuppressWarnings("WeakerAccess")
public class Benchmark {

  private static final char SEPARATOR = ',';
  private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

  protected final List<BenchmarkableSolver> solvers;
  private final Logger logger;

  private String resultFilePattern = "benchmark-%s.csv";  // default
  private boolean append = false;  // default
  private long timeoutValue;
  private TimeUnit timeoutUnit;
  protected BenchmarkSettings settings;

  public Benchmark() {
    solvers = new ArrayList<>();
    logger = LogManager.getLogger(this.getClass());
    setTimeout(60, TimeUnit.SECONDS);  // default
  }

  public void setResultFilePattern(String resultFilePattern) {
    this.setResultFilePattern(resultFilePattern, false);
  }

  public void setResultFilePattern(String resultFilePattern, boolean append) {
    if (!append && !resultFilePattern.contains("%(date)s")) {
      logger.warn("Ignoring (non-append) new file pattern lacking %(date)s part.");
      return;
    }
    this.append = append;
    this.resultFilePattern = resultFilePattern;
  }

  public void setTimeout(long timeoutValue, TimeUnit timeoutUnit) {
    this.timeoutValue = timeoutValue;
    this.timeoutUnit = timeoutUnit;
    for (BenchmarkableSolver s : solvers) {
      s.setTimeout(this.timeoutValue, this.timeoutUnit);
    }
  }

  public void setSettings(BenchmarkSettings settings) {
    this.settings = settings;
    setResultFilePattern(settings.resultFilePattern);
  }

  public void addSolver(BenchmarkableSolver s) {
    solvers.add(s);
    Configurator.setLevel(s.getClass().getPackage().getName(), Level.toLevel(this.settings.logLevel));
    s.setTimeout(this.timeoutValue, this.timeoutUnit);
  }

  protected StringBuilder createRow(ScenarioGenerator gen, int testId, Root model, long modelGeneration) {
    StringBuilder sb = new StringBuilder(makeNow()).append(SEPARATOR);
    return sb.append(testId).append(SEPARATOR)
        .append(gen.getNumTopLevelComponents()).append(SEPARATOR)
        .append(gen.getAvgNumImplSubComponents()).append(SEPARATOR)
        .append(gen.getImplSubComponentStdDerivation()).append(SEPARATOR)
        .append(gen.getAvgNumCompSubComponents()).append(SEPARATOR)
        .append(gen.getCompSubComponentStdDerivation()).append(SEPARATOR)
        .append(gen.getComponentDepth()).append(SEPARATOR)
        .append(gen.getNumImplementations()).append(SEPARATOR)
        .append(gen.getExcessResourceRatio()).append(SEPARATOR)
        .append(gen.getNumRequests()).append(SEPARATOR)
        .append(gen.getNumCpus()).append(SEPARATOR)
        .append(gen.getSeed()).append(SEPARATOR)
        .append(model.numComponents()).append(SEPARATOR)
        .append(model.numImplementations()).append(SEPARATOR)
        .append(modelGeneration).append(SEPARATOR)
        .append(gen.getInitialSolution().computeObjective()).append(SEPARATOR);
  }

  private void writeHeader(BufferedWriter writer) throws IOException {
    writer.append("when").append(SEPARATOR)
        .append("id").append(SEPARATOR)  // testId
        .append("tlc").append(SEPARATOR)  // topLevelComponents
        .append("isc").append(SEPARATOR)  // avgImplSubComponents
        .append("isd").append(SEPARATOR)  // implSubComponentStdDerivation
        .append("csc").append(SEPARATOR)  // avgCompSubComponents
        .append("csd").append(SEPARATOR)  // compSubComponentStdDerivation
        .append("dep").append(SEPARATOR)  // componentDepth
        .append("bi").append(SEPARATOR)  // implementations
        .append("res").append(SEPARATOR)  // computeResources
        .append("req").append(SEPARATOR)  // requests
        .append("cpu").append(SEPARATOR)  // cpus
        .append("seed").append(SEPARATOR)  // seed
        .append("comp").append(SEPARATOR)  // genComponents
        .append("impl").append(SEPARATOR)  // genImplementations
        .append("gen").append(SEPARATOR)  // modelGeneration
        .append("initObj").append(SEPARATOR)  // initial solution objective
        .append("name").append(SEPARATOR);  // name of the solver
    writeHeaderSolvers(writer);
    writer.append('\n');
  }

  protected void writeHeaderSolvers(BufferedWriter writer) throws IOException {
    writer.append("Gen").append(SEPARATOR)
          .append("Solved").append(SEPARATOR)
          .append("Obj").append(SEPARATOR)
          .append("Valid").append(SEPARATOR)
          .append("TimeOut");
  }

  /**
   * Run the benchmark with added solvers, writing to the set solution path.
   */
  public void run() {
    Objects.requireNonNull(settings, "Settings not set!");
    if (solvers.isEmpty()) {
      logger.warn("No solvers defined. Only model generation will be done.");
    }
    Path benchmarkPath = Paths.get(settings.path);
    Path path, modelDirectory, solutionDirectory;
    String start = makeNow();
    try {
      Path directory = benchmarkPath.resolve(getDirectory());
      createDirIfNecessary(directory);
      path = directory.resolve(stringFormat(resultFilePattern, e("date", start)));

      modelDirectory = directory.resolve("models");
      createDirIfNecessary(modelDirectory);

      solutionDirectory = directory.resolve("solutions");
      createDirIfNecessary(solutionDirectory);
    } catch (IOException e) {
      logger.catching(e);
      logger.fatal("Could not resolve directory: {}/{}", benchmarkPath, getDirectory());
      return;
    }
    AtomicInteger failCount = new AtomicInteger(5);
    final boolean needHeader = !Files.exists(path);
    try (BufferedWriter writer = openFile(path)) {
      if (needHeader) {
        writeHeader(writer);
      }
      TestGenerator testGen = new TestGenerator();
      testGen.setSettings(settings.basic);
      setTimeout(settings.basic.timeoutValue,
          TimeUnit.valueOf(settings.basic.timeoutUnit));

      // output a description of the benchmark
      StringBuilder sb = new StringBuilder();
      describe(sb, testGen);
      logger.info(sb.toString());

      AtomicInteger totalCount;
      if (settings.basic.total != null && settings.basic.total > 0) {
        totalCount = new AtomicInteger(settings.basic.total);
      } else {
        totalCount = null;
      }
      logger.info("Going to create {}{} models",
          totalCount == null ? "" : totalCount.get() + " of ",
          testGen.getExpectedModelCount());
      testGen.generateScenarioGenerator((gen, testId) -> runScenario(
          gen, testId, writer, path,
          () -> modelDirectory.resolve(stringFormat(settings.modelFilePattern,
              e("date", start), e("id", testId))),
          s -> solutionDirectory.resolve(stringFormat(settings.solutionFilePattern,
              e("date", start), e("solver", s.getName()), e("id", testId))),
          failCount, totalCount));
      logger.info("Results have been written to {}", path);
    } catch (IOException e) {
      logger.fatal("Could not create or write header to the benchmark file {}. {}. Exiting.",
          path.toAbsolutePath(), e);
    }
  }

  private String makeNow() {
    return df.format(new Date());
  }

  private void createDirIfNecessary(Path directory) throws IOException {
    if (!exists(directory)) {
      Files.createDirectories(directory);
    }
  }

  @SafeVarargs
  private final String stringFormat(String pattern, Map.Entry<String, Object>... args) {
    String result = pattern;
    for (Map.Entry<String, Object> entry : args) {
      result = result.replace("%(" + entry.getKey() + ")s", entry.getValue().toString());
    }
    return result;
  }

  private BufferedWriter openFile(Path path) throws IOException {
    if (append) {
      return Files.newBufferedWriter(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    } else {
      return Files.newBufferedWriter(path);
    }
  }

  protected String getDirectory() {
    return "basic";
  }

  protected boolean runScenario(ScenarioGenerator gen, int testId, BufferedWriter writer, Path path, ModelFilePattern mfp, SolutionFilePattern sfp, AtomicInteger failCount, AtomicInteger totalCount) {
    logger.debug("Starting model generation");
    StopWatch watch = StopWatch.start();
    Root model = gen.generate();
    logger.info("Model: {}", model.description());
    long modelGeneration = watch.time(TimeUnit.MILLISECONDS);
    saveModel(model, mfp.getModelPath());

    for (BenchmarkableSolver s : solvers) {
      // reset attribute values to have same start condition for all solvers
      model.flushAttrCache();
      StringBuilder sb = createRow(gen, testId, model, modelGeneration);
      Solution solution = solveAndAppend(model, s, sb);
      // write out solution
      try (BufferedWriter solutionWriter = Files.newBufferedWriter(sfp.getSolutionPath(s))) {
        MquatString out = solution.print(new MquatWriteSettings(" "));
        solutionWriter.write(out.toString());
      } catch (IOException e) {
        logger.catching(e);
      }
      if (!writeOutResult(writer, path, failCount, sb)) return false;
    }

    return totalCount == null || totalCount.decrementAndGet() > 0;
  }

  private void saveModel(Root model, Path modelPath) {
    try (BufferedWriter writer = Files.newBufferedWriter(modelPath)) {
      MquatString out = model.print(new MquatWriteSettings(" "));
      writer.write(out.toString());
    } catch (IOException e) {
      logger.catching(e);
    }
  }

  /**
   * Write out result. Return <code>true</code> if run should go on.
   * @param writer    target to write out
   * @param path      filename of target for error messages
   * @param failCount number of failed writes so far
   * @param sb        source to read from
   * @return <code>true</code> if everything was ok, <code>false</code> upon error
   */
  protected boolean writeOutResult(BufferedWriter writer, Path path, AtomicInteger failCount, StringBuilder sb) {
    try {
      writer.append(sb.toString());
      writer.flush();
    } catch (IOException e) {
      logger.error("Could not write to benchmark file " + path.toAbsolutePath(), e);
      if (failCount.decrementAndGet() == 0) {
        logger.fatal("Giving up to write to benchmark file.");
        return false;
      }
    }
    return true;
  }

  protected Solution solveAndAppend(Root model, BenchmarkableSolver s, StringBuilder sb) {
    Solution result = null;
    sb.append(s.getName()).append(SEPARATOR);
    try {
      logger.info("Calling solver '{}'", s.getName());
      result = s.solve(model);
      boolean validSolution = result.isValid();
      sb.append(s.doesGeneration() ? s.getLastGenerationTime() : -1).append(SEPARATOR)
          .append(s.getLastSolvingTime()).append(SEPARATOR)
          .append(s.getLastObjective()).append(SEPARATOR)
          .append(validSolution);
      logger.debug("Solver {} found {} solution in {}{}ms{}",
          s.getName(),
          validSolution ? "a valid" : "NO",
          s.doesGeneration() ? s.getLastGenerationTime() + " + " : "",
          s.getLastSolvingTime(),
          s.hadTimeout() ? " -> Timed out" : "");
    } catch (SolvingException e) {
      logger.catching(e);
      sb.append(-1).append(SEPARATOR)  // generation time
          .append(-1).append(SEPARATOR)  // solution time
          .append(-1).append(SEPARATOR)  // objective
          .append(false);  // valid
    }
    sb.append(SEPARATOR).append(s.hadTimeout())
        .append("\n");
    return result;
  }

  protected void describe(StringBuilder sb, TestGenerator testGen) {
    sb.append(this.getClass().getSimpleName()).append(":\n");
    sb.append("Timeout: ").append(this.timeoutValue).append(" ")
        .append(this.timeoutUnit.toString().toLowerCase()).append('\n');
    TestGeneratorSettings tgs = this.settings.basic;
    append(sb, "TopLevelComponents", tgs.minTopLevelComponents, tgs.maxTopLevelComponents);
    append(sb, "AvgNumImplSubComponents", tgs.minAvgNumImplSubComponents, tgs.maxAvgNumImplSubComponents);
    append(sb, "ImplSubComponentDerivation", tgs.minImplSubComponentDerivation, tgs.maxImplSubComponentDerivation);
    append(sb, "AvgNumCompSubComponents", tgs.minAvgNumCompSubComponents, tgs.maxAvgNumCompSubComponents);
    append(sb, "CompSubComponentDerivation", tgs.minCompSubComponentDerivation, tgs.maxCompSubComponentDerivation);
    append(sb, "ComponentDepth", tgs.minComponentDepth, tgs.maxComponentDepth);
    append(sb, "NumImplementations", tgs.minNumImplementations, tgs.maxNumImplementations);
    append(sb, "Requests", tgs.minRequests, tgs.maxRequests, tgs.stepRequests);
    append(sb, "Cpus", tgs.minCpus, tgs.maxCpus);
    append(sb, "ResourceRatio", tgs.minResourceRatio, tgs.maxResourceRatio, tgs.stepResourceRatio);
    append(sb, "Seed", this.settings.basic.seed);
    sb.append("Solvers: ").append(solvers.stream().map(Object::toString).collect(Collectors.joining(", ")))
        .append('\n');
  }

  protected <T extends Number> void append(StringBuilder sb, String name, T value) {
    append(sb, name, value, null, null);
  }

  protected <T extends Number> void append(StringBuilder sb, String name, T min, T max) {
    append(sb, name, min, max, null);
  }

  protected <T extends Number> void append(StringBuilder sb, String name, T min, T max, T step) {
    sb.append(name).append(": ");
    if (max == null || min.equals(max)) {
      sb.append(min);
    } else {
      sb.append("from ").append(min).append(" to ").append(max);
      if (step != null) {
        sb.append(" with step ").append(step);
      }
    }
    sb.append('\n');
  }

  interface ModelFilePattern {
    Path getModelPath();
  }

  interface SolutionFilePattern {
    Path getSolutionPath(BenchmarkableSolver s);
  }

}
