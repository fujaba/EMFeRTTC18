package de.tudresden.inf.st.mquat.utils;

import beaver.Parser;
import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import de.tudresden.inf.st.mquat.jastadd.parser.MquatParser;
import de.tudresden.inf.st.mquat.jastadd.scanner.MquatScanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Utility methods involving scanner and parser of the models.
 *
 * @author rschoene - Initial contribution
 */
public class ParserUtils {

  /**
   * Loads a model in a local file with the given path.
   * @param fileName path to the file, either absolute, or relative to the current directory
   * @return the parsed model
   * @throws IOException if the file could not be found, or opened
   * @throws Parser.Exception if the file contains a malformed model
   */
  public static Root load(String fileName) throws IOException, Parser.Exception {
    return load(fileName, null);
  }

  /**
   * Loads a model in a file with the given path.
   * Resolving of relative paths depends on the value of clazz.
   * @param fileName  path to the file, either absolute, or relative.
   * @param clazz     {@code null} to search relative to current directory, otherwise search relative to resources location of given class
   * @return the parsed model
   * @throws IOException if the file could not be found, or opened
   * @throws Parser.Exception if the file contains a malformed model
   */
  public static Root load(String fileName, Class<?> clazz) throws IOException, Parser.Exception {
    System.out.println("Loading model DSL file '" + fileName + "'.");
    Reader reader = clazz == null ? getLocalReaderFor(fileName) : getClassReaderFor(fileName, clazz);
    MquatScanner scanner = new MquatScanner(reader);
    MquatParser parser = new MquatParser();
    Root result = (Root) parser.parse(scanner);
    parser.resolveReferences();
    reader.close();
    return result;
  }

  /**
   * Loads a solution in a local file with the given path.
   * @param fileName  path to the file, either absolute, or relative to the current directory
   * @param model     the referenced model
   * @return the parsed solution
   * @throws IOException if the file could not be found, or opened
   * @throws Parser.Exception if the file contains a malformed model
   */
  public static Solution loadSolution(String fileName, Root model) throws IOException, Parser.Exception {
    return loadSolution(fileName, model, null);
  }

  /**
   * Loads a solution in a file with the given path.
   * Resolving of relative paths depends on the value of clazz.
   * @param fileName  path to the file, either absolute, or relative
   * @param model     the referenced model
   * @param clazz     {@code null} to search relative to current directory, otherwise search relative to resources location of given class
   * @return the parsed solution
   * @throws IOException if the file could not be found, or opened
   * @throws Parser.Exception if the file contains a malformed model
   */
  public static Solution loadSolution(String fileName, Root model, Class<?> clazz) throws IOException, Parser.Exception {
    System.out.println("Loading solution DSL file '" + fileName + "'.");
    Reader reader = clazz == null ? getLocalReaderFor(fileName) : getClassReaderFor(fileName, clazz);
    MquatScanner scanner = new MquatScanner(reader);
    MquatParser parser = new MquatParser();
    Solution result = (Solution) parser.parse(scanner, MquatParser.AltGoals.solution);
    parser.resolveSolutionReferencesWith(model);
    reader.close();
    return result;
  }

  private static Reader getClassReaderFor(String fileName, Class<?> clazz) throws IOException {
    URL url = clazz.getClassLoader().getResource(fileName);
    return new BufferedReader(new InputStreamReader(
        Objects.requireNonNull(url, "Could not open file " + fileName)
            .openStream()));
  }

  private static Reader getLocalReaderFor(String fileName) throws IOException {
    try {
      return Files.newBufferedReader(Paths.get(fileName));
    } catch (IOException e) {
      System.out.println("Error. Searching at" + Paths.get(fileName).toAbsolutePath());
      throw e;
    }
  }
}
