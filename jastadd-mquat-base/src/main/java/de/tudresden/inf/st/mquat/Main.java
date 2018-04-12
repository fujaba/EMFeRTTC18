package de.tudresden.inf.st.mquat;

import beaver.Parser;
import de.tudresden.inf.st.mquat.deserializer.ASTNodeDeserializer;
import de.tudresden.inf.st.mquat.generator.*;
import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.jastadd.parser.MquatParser;
import de.tudresden.inf.st.mquat.jastadd.scanner.MquatScanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Main test entry point for jastadd-mquat.
 * Created by rschoene on 11/01/17.
 */
@SuppressWarnings("unused")
public class Main {

  public static final ScenarioDescription SCENARIO_DESCRIPTION = new ScenarioDescription(2, 2, 0, 0, 0, 2, 2, 1.5, 2, 2, 0);

  private static File getAbsoluteFileForLoading(String fileName) throws FileNotFoundException {
    URL expUrl = Main.class.getClassLoader().getResource(fileName);
    File file;
    if (expUrl != null) {
      file = new File(expUrl.getFile());
    } else {
      file = new File(fileName);
    }
    if (!file.exists()) {
      throw new FileNotFoundException("Could not find file " + fileName);
    }
    return file;
  }

  private static Root load(String fileName) throws IOException, Parser.Exception {
    File file = getAbsoluteFileForLoading(fileName);
    if (fileName.endsWith(".json")) {
      System.out.println("Loading JSON file '" + fileName + "'.");
      return ASTNodeDeserializer.read(file);
    } else {
      System.out.println("Loading expression DSL file '" + fileName + "'.");
      FileReader reader = new FileReader(file);
      MquatScanner scanner = new MquatScanner(reader);
      MquatParser parser = new MquatParser();
      Root result = (Root) parser.parse(scanner);
      parser.resolveReferences();
      return result;
    }
  }

  /**
   * Print the node, and stores the output in a file.
   * The file is created and truncated first, if needed.
   * @param node     the node to print
   * @param settings how to print the node (can be <code>null</code> if node is an ILP
   * @param fileName where to store the output
   */
  public static void write(ASTNode<?> node, MquatWriteSettings settings, String fileName)
          throws IOException {
    String output;
    if (node instanceof ILP) {
      output = ((ILP) node).printIlp().toString();
    } else {
      output = node.print(settings).toString();
    }
    Path path = Paths.get(fileName);
    System.out.println("Writing " + node.getClass().getSimpleName() + " to " + path.toAbsolutePath());
    try (BufferedWriter writer = Files.newBufferedWriter(
            path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      writer.write(output);
    }
  }

  private static void printFromProcess(Process process) {
    try (Scanner s = new Scanner(process.getInputStream())) {
      System.out.println(s.useDelimiter("\\A").hasNext() ? s.next() : "");
    }
    try (Scanner s = new Scanner(process.getErrorStream())) {
      System.err.println(s.useDelimiter("\\A").hasNext() ? s.next() : "");
    }
  }

  // required for the DrAST debugger
  public static Object DrAST_root_node;

  public static Optional<Root> loadModel(String fileName) {
    try {
      Root root = load(fileName);
      // required for the DrAST debugger
      DrAST_root_node = root;
      System.out.println(root.info());
      return Optional.of(root);
    } catch (IOException | Parser.Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  private static void checkParsedModel(Root parsedModel, File originalFile, MquatWriteSettings settings)
      throws IOException, InterruptedException {
    if (parsedModel == null) {
      System.err.println("Passed model is null. Parsing failed!");
      return;
    }
    String parsedFileName = originalFile.getAbsolutePath().replace(".txt", "-parsed.txt");
    write(parsedModel, settings, parsedFileName);
    Process process = Runtime.getRuntime().exec(
        "diff --ignore-trailing-space " + originalFile.getAbsolutePath() + " " + parsedFileName);
    int returnCode = process.waitFor();
    if (returnCode == 0) {
      System.out.println("Models match!");
    } else {
      printFromProcess(process);
    }
  }

  private static Root generateNewModel(MquatWriteSettings settings, boolean printModel) throws IOException {
    ScenarioGenerator generator = new ScenarioGenerator(SCENARIO_DESCRIPTION);
    Root generatedModel = generator.generate();
    if (printModel) {
      System.out.println("---");
      System.out.println(generatedModel.print(settings));
    }
    generator.printInfo();
    write(generatedModel, settings, "src/main/resources/model-0.txt");
    return generatedModel;
  }

  public static void main(String[] args) throws Exception {
    Logger logger = LogManager.getLogger(Main.class);
    logger.info("Starting base.Main");
//    String fileName = args.length > 0 ? args[0] : "model-handmade.txt";
//    Optional<Root> parsedModel = loadModel(fileName);
//    checkParsedModel(parsedModel.orElseThrow(RuntimeException::new), getAbsoluteFileForLoading(fileName), settings);
    ExtensibleScenarioGenerator esg = new ExtensibleScenarioGenerator();
    esg.setDescription(SCENARIO_DESCRIPTION);
    esg.setSerializer(new LoggingSerializer());
    esg.generateModel();
  }
}

