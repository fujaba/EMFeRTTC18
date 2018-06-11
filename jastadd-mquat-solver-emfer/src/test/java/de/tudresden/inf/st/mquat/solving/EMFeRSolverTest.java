package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.generator.ScenarioDescription;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import de.tudresden.inf.st.mquat.jastadd.model.Root;
import de.tudresden.inf.st.mquat.jastadd.model.Solution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import uniks.EMFeRSolver;

import java.io.IOException;
import java.nio.file.*;

public class EMFeRSolverTest {

  private static Logger logger;

  @BeforeClass
  public static void initLogger() {
    logger = LogManager.getLogger(EMFeRSolverTest.class);
  }

  /**
   * tests the simple solver with one very simple use case
   */
  @Test
  public void testEmferSolver() throws SolvingException {
    int tlc = 1;
    int iac = 0;
    int isd = 0;
    int cac = 2;
    int csd = 0;
    int dep = 5;
    int imp = 2;
    int mod = 3;
    double res = 1.5d;
    int nfp = 0;
    int req = 1;
    int cpu = 1;
    int seed = 0;

    ScenarioGenerator generator = new ScenarioGenerator(new ScenarioDescription(tlc, iac, isd, cac, csd, dep, imp, res, req, cpu, seed));

    Root model = generator.generate();

    EMFeRSolver solver = new EMFeRSolver(20000);

    Solution solution = solver.solve(model);

    Assert.assertNotNull(solution);

    logger.info("the best solution is {} and has an objective of {}.", (solution.isValid() ? "valid" : "invalid"), solution.computeObjective());

  }

  @Test
  public void testListener()
  {
    Path path = Paths.get(".");

    try
    {
      WatchService watcher = FileSystems.getDefault().newWatchService();
      path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

      Files.write(Paths.get("./message.txt"), "Hello World".getBytes());

      WatchKey event = watcher.take();

      for (WatchEvent we : event.pollEvents())
      {
         System.out.println("" + we.kind() + " " + we.context());

         String context = we.context().toString();

         String content = new String(Files.readAllBytes(Paths.get(context)));

         System.out.println(content);
      }

    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

}
