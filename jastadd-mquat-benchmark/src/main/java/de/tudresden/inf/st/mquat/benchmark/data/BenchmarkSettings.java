package de.tudresden.inf.st.mquat.benchmark.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tudresden.inf.st.mquat.data.TestGeneratorSettings;

import java.util.List;

import static de.tudresden.inf.st.mquat.benchmark.Utils.nonNullOrDefault;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BenchmarkSettings {

  public String kind = null;
  public String path = null;
  public String resultFilePattern = null;
  public String modelFilePattern = null;
  public String solutionFilePattern = null;
  public List<String> solvers = null;
  public String logLevel = null;

  public final TestGeneratorSettings basic = new TestGeneratorSettings();

  public void update(BenchmarkSettings other) {
    this.kind = nonNullOrDefault(other.kind, this.kind);
    this.path = nonNullOrDefault(other.path, this.path);
    this.resultFilePattern = nonNullOrDefault(other.resultFilePattern, this.resultFilePattern);
    this.solvers = nonNullOrDefault(other.solvers, this.solvers);
    this.logLevel = nonNullOrDefault(other.logLevel, this.logLevel);
    updateBasic(other.basic);
  }

  public void updateBasic(TestGeneratorSettings other) {
    basic.verbose = nonNullOrDefault(other.verbose, basic.verbose);
    basic.minTopLevelComponents = nonNullOrDefault(other.minTopLevelComponents, basic.minTopLevelComponents);
    basic.maxTopLevelComponents = nonNullOrDefault(other.maxTopLevelComponents, basic.maxTopLevelComponents);
    basic.minAvgNumImplSubComponents = nonNullOrDefault(other.minAvgNumImplSubComponents, basic.minAvgNumImplSubComponents);
    basic.maxAvgNumImplSubComponents = nonNullOrDefault(other.maxAvgNumImplSubComponents, basic.maxAvgNumImplSubComponents);
    basic.minImplSubComponentDerivation = nonNullOrDefault(other.minImplSubComponentDerivation, basic.minImplSubComponentDerivation);
    basic.maxImplSubComponentDerivation = nonNullOrDefault(other.maxImplSubComponentDerivation, basic.maxImplSubComponentDerivation);
    basic.minAvgNumCompSubComponents = nonNullOrDefault(other.minAvgNumCompSubComponents, basic.minAvgNumCompSubComponents);
    basic.maxAvgNumCompSubComponents = nonNullOrDefault(other.maxAvgNumCompSubComponents, basic.maxAvgNumCompSubComponents);
    basic.minCompSubComponentDerivation = nonNullOrDefault(other.minCompSubComponentDerivation, basic.minCompSubComponentDerivation);
    basic.maxCompSubComponentDerivation = nonNullOrDefault(other.maxCompSubComponentDerivation, basic.maxCompSubComponentDerivation);
    basic.minComponentDepth = nonNullOrDefault(other.minComponentDepth, basic.minComponentDepth);
    basic.maxComponentDepth = nonNullOrDefault(other.maxComponentDepth, basic.maxComponentDepth);
    basic.minNumImplementations = nonNullOrDefault(other.minNumImplementations, basic.minNumImplementations);
    basic.maxNumImplementations = nonNullOrDefault(other.maxNumImplementations, basic.maxNumImplementations);
    basic.minRequests = nonNullOrDefault(other.minRequests, basic.minRequests);
    basic.maxRequests = nonNullOrDefault(other.maxRequests, basic.maxRequests);
    basic.stepRequests = nonNullOrDefault(other.stepRequests, basic.stepRequests);
    basic.minCpus = nonNullOrDefault(other.minCpus, basic.minCpus);
    basic.maxCpus = nonNullOrDefault(other.maxCpus, basic.maxCpus);
    basic.minResourceRatio = nonNullOrDefault(other.minResourceRatio, basic.minResourceRatio);
    basic.maxResourceRatio = nonNullOrDefault(other.maxResourceRatio, basic.maxResourceRatio);
    basic.stepResourceRatio = nonNullOrDefault(other.stepResourceRatio, basic.stepResourceRatio);
    basic.timeoutValue = nonNullOrDefault(other.timeoutValue, basic.timeoutValue);
    basic.timeoutUnit = nonNullOrDefault(other.timeoutUnit, basic.timeoutUnit);
    basic.seed = nonNullOrDefault(other.seed, basic.seed);
    basic.total = nonNullOrDefault(other.total , basic.total);
  }

}
