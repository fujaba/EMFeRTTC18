package de.tudresden.inf.st.mquat.solving.ilp;

import de.tudresden.inf.st.mquat.generator.ScenarioDescription;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import de.tudresden.inf.st.mquat.jastadd.model.ILP;
import de.tudresden.inf.st.mquat.jastadd.model.MquatWriteSettings;
import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import de.tudresden.inf.st.mquat.solving.Solver;
import de.tudresden.inf.st.mquat.solving.SolvingException;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gnu.glpk.GLPK;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static de.tudresden.inf.st.mquat.Main.write;

public class ILPMain {

  private static void printFromProcess(Process process, boolean printError) {
    try (Scanner s = new Scanner(process.getInputStream())) {
      System.out.println(s.useDelimiter("\\A").hasNext() ? s.next() : "");
    }
    if (printError) {
      try (Scanner s = new Scanner(process.getErrorStream())) {
        System.err.println(s.useDelimiter("\\A").hasNext() ? s.next() : "");
      }
    }
  }

  private static ILP generateILP(Root generatedModel) throws IOException {
    StopWatch watch = StopWatch.start();
    ILP ilp = generatedModel.getILP();
    System.out.flush();
    System.err.flush();
//    System.out.println("---");
//    System.out.println(ilp.printIlp());
//    System.out.println("---");
//    System.out.println(ilp.printIlp());
//    System.out.println("---");
    write(ilp, null, "src/main/resources/sample.lp");
    System.out.println("Generation took " + watch.time(TimeUnit.MILLISECONDS) + "ms.");
    return ilp;
  }

  private static void solveILP(ILP ilp) throws IOException, InterruptedException {
    StopWatch watch = StopWatch.start();
    Process process;
    String solutionPath = Paths.get("src", "main", "resources", "solution.txt").toAbsolutePath().toString();
    process = Runtime.getRuntime().exec(
        "glpsol --lp src/main/resources/sample.lp -o " + solutionPath + " -w " + solutionPath + ".mr",
        null, new File("."));
    if (!process.waitFor(1, TimeUnit.MINUTES)) {
      process.destroyForcibly();
      System.out.println("Timeout for solving!");
    } else {
      System.out.println(process.exitValue());
      printFromProcess(process, false);
    }
    System.out.println("Solving took " + watch.time(TimeUnit.MILLISECONDS) + "ms.");

    // parse the solution and print variables not assigned zero
    process = Runtime.getRuntime().exec(
        "src/main/python/parse_solution.py " + solutionPath,
        null, new File("."));
    process.waitFor();
    printFromProcess(process, true);
  }

  private static void solveILPWithSolver(Root model) throws SolvingException {
    ILPExternalSolver solver = new ILPExternalSolver();
    Solution solution = solver.solve(model);
    System.out.println(solution);
  }

  public static void main(String[] args) throws Exception {
//    System.out.println("Solving tiny model");
//    Optional<Root> tinyModel = loadModel("tiny.txt");
//    ILP tinyIlp = generateILP(tinyModel.orElseThrow(RuntimeException::new));
//    solveILP(tinyIlp);
//    solveILPWithSolver(tinyModel.orElseThrow(RuntimeException::new));
    Logger logger = LogManager.getLogger(ILPMain.class);
    String version = GLPK.glp_version();
    System.out.println(version);
    ScenarioGenerator gen = new ScenarioGenerator(new ScenarioDescription(1, 2, 0, 0, 0, 2, 2, 2.5, 3, 1, 0));
    Root model = gen.generate();
    Solver external = new ILPExternalSolver().setDeleteFilesOnExit(false);
    Solution solution = external.solve(model);
    logger.info(model.print(new MquatWriteSettings(" ")));
    solution.explain();
  }

}
