package de.tudresden.inf.st.mquat.data;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TestGeneratorSettings {

  public Boolean verbose = null;

  public Integer minTopLevelComponents = null;
  public Integer maxTopLevelComponents = null;

  public Integer minAvgNumImplSubComponents = null;
  public Integer maxAvgNumImplSubComponents = null;

  public Integer minImplSubComponentDerivation = null;
  public Integer maxImplSubComponentDerivation = null;

  public Integer minAvgNumCompSubComponents = null;
  public Integer maxAvgNumCompSubComponents = null;

  public Integer minCompSubComponentDerivation = null;
  public Integer maxCompSubComponentDerivation = null;

  public Integer minComponentDepth = null;
  public Integer maxComponentDepth = null;

  public Integer minNumImplementations = null;
  public Integer maxNumImplementations = null;

  public Integer minRequests = null;
  public Integer maxRequests = null;
  public Integer stepRequests = null;

  public Integer minCpus = null;
  public Integer maxCpus = null;

  public Double minResourceRatio = null;
  public Double maxResourceRatio = null;
  public Double stepResourceRatio = null;

  public Integer timeoutValue = null;
  public String timeoutUnit = null;

  public Integer seed = null;

  public Integer total = null;
  public boolean shouldExitOnWarnings = true;
}
