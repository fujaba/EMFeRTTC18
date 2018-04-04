package de.tudresden.inf.st.mquat;

import beaver.Parser;
import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.jastadd.scanner.MquatScanner;
import de.tudresden.inf.st.mquat.utils.ParserUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * TODO: Add description.
 *
 * @author rschoene - Initial contribution
 */
public class MainCheck {

  public static void main(String[] args) {
    System.out.println(Arrays.toString(args));
    if (args.length != 2) {
      printUsage();
      System.exit(1);
    }
    String modelFileName = args[0];
    String solutionFileName = args[1];
    Solution solution;
    try {
      Root model = ParserUtils.load(modelFileName);
      solution = ParserUtils.loadSolution(solutionFileName, model);
    } catch (IOException | Parser.Exception e) {
      e.printStackTrace();
      return;
    }
    MquatString out = solution.print(new MquatWriteSettings(" "));
    System.out.println(out);
    boolean isValid = solution.isValid();
    double objectiveValue = solution.computeObjective();
    System.out.println("Solution valid: " + Boolean.toString(isValid));
    System.out.println("Objective value: " + objectiveValue);
  }

  private static void printUsage() {
    System.out.println("Error. Need to be called with two arguments: ModelFile and SolutionFile!");
  }
}
