package de.tudresden.inf.st.mquat.utils;

import de.tudresden.inf.st.mquat.data.TestGeneratorSettings;
import de.tudresden.inf.st.mquat.generator.ScenarioDescription;
import de.tudresden.inf.st.mquat.generator.ScenarioGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates a number of ScenarioGenerators by iterating the range of each parameter.
 */
public class TestGenerator {
  private TestGeneratorSettings settings;

  private Logger logger;

  public TestGenerator() {
    logger = LogManager.getLogger(TestGenerator.class);
  }

  public TestGenerator(TestGeneratorSettings settings) {
    this();
    setSettings(settings);
  }

  public int generateScenarioGenerator(ScenarioGeneratorConsumer consumer) {
    if (hasWarnings() && settings.shouldExitOnWarnings) {
      logger.error("Exiting because of previous warnings.");
      return 0;
    }
    int testId = 0;
    for (int tlc = settings.minTopLevelComponents; tlc <= settings.maxTopLevelComponents; tlc++) {
      for (int iac = settings.minAvgNumImplSubComponents; iac <= settings.maxAvgNumImplSubComponents; iac++) {
        for (int isd = settings.minImplSubComponentDerivation; isd <= settings.maxImplSubComponentDerivation; isd++) {
          for (int cac = settings.minAvgNumCompSubComponents; cac <= settings.maxAvgNumCompSubComponents; cac++) {
            for (int csd = settings.minCompSubComponentDerivation; csd <= settings.maxCompSubComponentDerivation; csd++) {
              for (int dep = settings.minComponentDepth; dep <= settings.maxComponentDepth; dep++) {
                for (int imp = settings.minNumImplementations; imp <= settings.maxNumImplementations; imp++) {
                  for (double res = settings.minResourceRatio; res <= settings.maxResourceRatio; res += settings.stepResourceRatio) {
                    for (int req = settings.minRequests; req <= settings.maxRequests; req += settings.stepRequests) {
                      for (int cpu = settings.minCpus; cpu <= settings.maxCpus; cpu++) {
                        if (settings.verbose) {
                          logger.debug(
                              "Test " + testId + " with tlc=" + tlc
                                  + ", iac=" + iac
                                  + ", isd=" + isd
                                  + ", cac=" + cac
                                  + ", csd=" + csd
                                  + ", dep=" + dep
                                  + ", imp=" + imp
//                                      + ", mod=" + mod
                                  + ", res=" + res
//                                      + ", nfp=" + nfp
                                  + ", req=" + req
                                  + ", cpu=" + cpu
                                  + ", seed=" + settings.seed);
                        }
                        ScenarioGenerator generator = new ScenarioGenerator(new ScenarioDescription(
                            tlc, iac, isd, cac, csd, dep, imp, res, req, cpu, settings.seed));
                        if (!consumer.run(generator, testId++)) {
                          return testId;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return testId;
  }

  public void setSettings(TestGeneratorSettings settings) {
    this.settings = settings;
  }

  public int getExpectedModelCount() {
    return (settings.maxTopLevelComponents + 1 - settings.minTopLevelComponents) *
        (settings.maxAvgNumImplSubComponents + 1 - settings.minAvgNumImplSubComponents) *
        (settings.maxImplSubComponentDerivation + 1 - settings.minImplSubComponentDerivation) *
        (settings.maxAvgNumCompSubComponents + 1 - settings.minAvgNumCompSubComponents) *
        (settings.maxCompSubComponentDerivation + 1 - settings.minCompSubComponentDerivation) *
        (settings.maxComponentDepth + 1 - settings.minComponentDepth) *
        (settings.maxNumImplementations + 1 - settings.minNumImplementations) *
        (settings.maxCpus + 1 - settings.minCpus) *
        (int) ((settings.maxResourceRatio - settings.minResourceRatio) / settings.stepResourceRatio + 1) *
        (int) ((settings.maxRequests - settings.minRequests) / 1.0 * settings.stepRequests + 1);
  }

  /**
   * Checks all given parameters w.r.t. their validity, e.g., whether min &lt; max, or min &gt; max if step &lt; 0
   * @return whether the this generator has wrong parameters
   */
  private boolean hasWarnings() {
    boolean hasWarnings = false;
    if (settings.minTopLevelComponents > settings.maxTopLevelComponents) {
      logger.warn("minTopLevelComponents ({}) is greater than its max counterpart ({})!",
          settings.minTopLevelComponents, settings.maxTopLevelComponents);
      hasWarnings = true;
    }

    if (settings.minAvgNumImplSubComponents > settings.maxAvgNumImplSubComponents) {
      logger.warn("minAvgNumImplSubComponents ({}) is greater than its max counterpart ({})!",
          settings.minAvgNumImplSubComponents, settings.maxAvgNumImplSubComponents);
      hasWarnings = true;
    }

    if (settings.minImplSubComponentDerivation > settings.maxImplSubComponentDerivation) {
      logger.warn("minImplSubComponentDerivation ({}) is greater than its max counterpart ({})!",
          settings.minImplSubComponentDerivation, settings.maxImplSubComponentDerivation);
      hasWarnings = true;
    }

    if (settings.minAvgNumCompSubComponents > settings.maxAvgNumCompSubComponents) {
      logger.warn("minAvgNumCompSubComponents ({}) is greater than its max counterpart ({})!",
          settings.minAvgNumCompSubComponents, settings.maxAvgNumCompSubComponents);
      hasWarnings = true;
    }

    if (settings.minCompSubComponentDerivation > settings.maxCompSubComponentDerivation) {
      logger.warn("minCompSubComponentDerivation ({}) is greater than its max counterpart ({})!",
          settings.minCompSubComponentDerivation, settings.maxCompSubComponentDerivation);
      hasWarnings = true;
    }

    if (settings.minComponentDepth > settings.maxComponentDepth) {
      logger.warn("minComponentDepth ({}) is greater than its max counterpart ({})!",
          settings.minComponentDepth, settings.maxComponentDepth);
      hasWarnings = true;
    }

    if (settings.minNumImplementations > settings.maxNumImplementations) {
      logger.warn("minNumImplementations ({}) is greater than its max counterpart ({})!",
          settings.minNumImplementations, settings.maxNumImplementations);
      hasWarnings = true;
    }

    if (settings.minResourceRatio > settings.maxResourceRatio && settings.stepResourceRatio >= 0) {
      logger.warn("minResourceRatio ({}) is greater than its max counterpart ({}) with step = {}!",
          settings.minResourceRatio, settings.maxResourceRatio, settings.stepResourceRatio);
      hasWarnings = true;
    } else if (settings.minResourceRatio > settings.maxResourceRatio && settings.stepResourceRatio >= 0) {
      logger.warn("minResourceRatio ({}) is smaller than its max counterpart ({}) with step = {}!",
          settings.minResourceRatio, settings.maxResourceRatio, settings.stepResourceRatio);
      hasWarnings = true;
    }

    if (settings.minCpus > settings.maxCpus) {
      logger.warn("maxCpu ({}) is greater than its max counterpart ({})!",
          settings.minCpus, settings.maxCpus);
      hasWarnings = true;
    }

    if (settings.minRequests > settings.maxRequests && settings.stepRequests >= 0) {
      logger.warn("minRequests ({}) is greater than its max counterpart ({}) with step = {}!",
          settings.minRequests, settings.maxRequests, settings.stepRequests);
      hasWarnings = true;
    } else if (settings.minRequests < settings.maxRequests && settings.stepRequests <= 0) {
      logger.warn("minRequests ({}) is smaller than its max counterpart ({}) with step = {}!",
          settings.minRequests, settings.maxRequests, settings.stepRequests);
      hasWarnings = true;
    }
    return hasWarnings;
  }

}
