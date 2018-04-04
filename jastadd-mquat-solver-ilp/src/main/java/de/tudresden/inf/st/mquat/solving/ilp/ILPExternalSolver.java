package de.tudresden.inf.st.mquat.solving.ilp;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.solving.SolvingException;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ILPExternalSolver extends AbstractILPSolver {

  private boolean deleteFilesOnExit;
  private Path lp, solutionReadable;

  /**
   * Create a new solver with default settings.
   * Default is:
   * <ul>
   *   <li>1 minute timeout</li>
   *   <li>delete temporary files on exit.</li>
   * </ul>
   * @see ILPExternalSolver#setDeleteFilesOnExit(boolean)
   */
  public ILPExternalSolver() {
    super(LogManager.getLogger(ILPExternalSolver.class));
    deleteFilesOnExit = true;
  }

  public ILPExternalSolver setDeleteFilesOnExit(boolean deleteFilesOnExit) {
    this.deleteFilesOnExit = deleteFilesOnExit;
    return this;
  }

  /**
   * Log stdout (always to logger.debug) and stderr (if existing to logger.warn)
   * @param process the given process to inspect
   */
  private void printFromProcess(Process process) {
    try (Scanner s = new Scanner(process.getInputStream())) {
      logger.debug(s.useDelimiter("\\A").hasNext() ? s.next() : "<no output>");
    }
    try (Scanner s = new Scanner(process.getErrorStream())) {
      if (s.useDelimiter("\\A").hasNext()) {
        logger.warn(s.next());
      }
    }
  }

  @Override
  protected void cleanup(StopWatch watch) {
    super.cleanup(watch);
    if (deleteFilesOnExit) {
      if (lp.toFile().exists() && !lp.toFile().delete()) {
        logger.warn("Could not delete ILP file {}", lp.toAbsolutePath());
      }
      if (solutionReadable.toFile().exists() && !solutionReadable.toFile().delete()) {
        logger.warn("Could not delete solution file {}", solutionReadable.toAbsolutePath());
      }
    }
  }

  protected double solve0(Root model, StopWatch watch, List<IlpVariable> variablesSetToOne) throws SolvingException {
    // Create temporary files
    try {
      lp = Files.createTempFile("ilp", null);
//      solution = Files.createTempFile("solution", null);
      solutionReadable = Files.createTempFile("sol-read", null);
    } catch (IOException e) { throw new SolvingException("Can not create lp or solution file", e); }
    if (!deleteFilesOnExit) {
      logger.info("Writing ILP to {}, solving now", lp.toAbsolutePath());
    }

    // write out lp file
    IlpString output = model.getILP().printIlp();
    try (BufferedWriter writer = Files.newBufferedWriter(
        lp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      writer.write(output.toString());
    } catch (IOException e) { cleanup(watch); throw new SolvingException("Could not write to lp file", e); }

    // start GLPK to solve the lp file just written, writing out the solution
    Process process;
    String command = "glpsol --lp " + lp.toAbsolutePath() +
//        " -w " + solution.toAbsolutePath() +
        " --tmlim " + timeoutInSeconds +
        " -o " + solutionReadable.toAbsolutePath();
    logger.debug("Call: '{}'", command);
    try {
      process = Runtime.getRuntime().exec(command,null, new File("."));
    } catch (IOException e) { cleanup(watch); throw new SolvingException("Problem calling glpsol. Is it installed?", e); }
    boolean finishedInTime;
    try {
      finishedInTime = process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      cleanup(watch);
      throw new SolvingException("Interrupted while waiting for result", e);
    }
    if (!finishedInTime) {
      // solver already had a timeout, so wait at least 2 seconds longer to let it write a solution file
      this.timedOut = true;
      try {
        process.waitFor(2, TimeUnit.SECONDS);
      } catch (InterruptedException ignored) { }
      // then destroy the process
      process.destroyForcibly();
      if (!solutionReadable.toFile().exists()) {
        cleanup(watch);
        throw new SolvingException("Solving did not finish within " + timeoutValue + " " + timeoutUnit.toString());
      }
      // if there is a solution file, move on and check its content
    }
    printFromProcess(process);
    if (!solutionReadable.toFile().exists()) {
      cleanup(watch);
      throw new SolvingException("No solution file was created.");
    }
    logger.debug("Solution at {}", solutionReadable);

    // read the solution file
    ILPSolution result = new ILPSolution(model);

//    readFromPrintableSolution(ilp, solution, result, variablesSetToOne);
    readFromPlainTextSolution(model.getILP().getInfo(), solutionReadable, result, variablesSetToOne);
    return result.getObjective();
  }

  private static void readFromPlainTextSolution(IlpVarInfo info, Path solution, ILPSolution result,
                                                List<IlpVariable> variablesSetToOne) throws SolvingException {
    List<String> varNamesSetToOne = new ArrayList<>();
    String name = null;
    int phase = 1;
    try (Stream<String> lines = Files.lines(solution)) {
      for (String line : lines.collect(Collectors.toList())) {
        if (phase < 3) {
          if (line.startsWith("Objective")) {
            int equalsIndex = line.indexOf('=');
            int bracketIndex = line.lastIndexOf('(');
            result.setObjective(Double.valueOf(line.substring(equalsIndex + 1, bracketIndex).trim()));
          }
          if (line.startsWith("---")) {
            phase += 1;
          }
          continue;
        }
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
        String[] tokens = line.split("\\s+");
        if (tokens.length == 6) {
          // tokens: index, name, star, activity, lb, rb
          if(Integer.valueOf(tokens[3]) == 1) {
            varNamesSetToOne.add(tokens[1]);
          }
          phase = 3;
        } else if (phase == 3) {
          if(line.startsWith("Integer")) {
            break;
          }
          // tokens: index, name
          name = tokens[1];
          phase = 4;
        } else if (phase == 4) {
          // tokens: star, activity, lb, rb
          if (name == null) {
            throw new SolvingException("Error in parsing solution. Name is null. Tokens: " + Arrays.toString(tokens));
          }
          if (Integer.valueOf(tokens[1]) == 1) {
            varNamesSetToOne.add(name);
            name = null;
          }
          phase = 3;
        }
      }
    } catch (IOException e) {
      throw new SolvingException("Could not open solution file", e);
    } catch (NumberFormatException | IndexOutOfBoundsException e) {
      throw new SolvingException("Could not parse solution file", e);
    }
    for (String varName : varNamesSetToOne) {
      IlpVariable variable = info.vars.get(varName);
      if (variable == null) {
        throw new SolvingException("Could not find variable with name " + varName);
      }
      variablesSetToOne.add(variable);
    }
  }

  @Override
  public String getName() {
    return "ilp-external";
  }

}
