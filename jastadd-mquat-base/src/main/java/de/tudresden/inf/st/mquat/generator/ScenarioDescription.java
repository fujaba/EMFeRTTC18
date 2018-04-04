package de.tudresden.inf.st.mquat.generator;

/**
 * A class to contain all scenario parameter for the problem domain
 *
 * @author rschoene - Initial contribution
 */
public class ScenarioDescription {
  public final long seed;
  public final int avgNumImplSubComponents;
  public final int implSubComponentStdDerivation;
  public final int avgNumCompSubComponents;
  public final int compSubComponentStdDerivation;
  public final int numImplementations;
  public final double excessComputeResourceRatio;
  public final int numRequests;
  public final int numTopLevelComponents;
  public final int componentDepth;
  public final int numCpus;

  /**
   * A class to contain all scenario parameter for the problem domain
   *
   * @param numTopLevelComponents          the amount of components that are used in requests
   * @param avgNumImplSubComponents        the average number of required components each implementation in every level except the last
   * @param implSubComponentStdDerivation  the standard derivation of the average number of components for an implementation
   * @param avgNumCompSubComponents        the average number of required components each component in every level except the last
   * @param compSubComponentStdDerivation  the standard derivation of the average number of components for a component
   * @param componentDepth                 the depth of the required component tree
   * @param numImplementations             the amount of numImplementations for each component
   * @param excessComputeResourceRatio     the factor of how many more compute nodes there are than required
   * @param numRequests                    the amount of requests
   * @param numCpus                        the number of CPUs per component
   * @param seed                           the random seed used to get the numbers
   */
  public ScenarioDescription(int numTopLevelComponents, int avgNumImplSubComponents, int implSubComponentStdDerivation, int avgNumCompSubComponents, int compSubComponentStdDerivation, int componentDepth, int numImplementations, double excessComputeResourceRatio, int numRequests, int numCpus, long seed) {
    this.numTopLevelComponents = numTopLevelComponents;
    this.avgNumImplSubComponents = avgNumImplSubComponents;
    this.implSubComponentStdDerivation = implSubComponentStdDerivation;
    this.avgNumCompSubComponents = avgNumCompSubComponents;
    this.compSubComponentStdDerivation = compSubComponentStdDerivation;
    this.numImplementations = numImplementations;
    this.componentDepth = componentDepth;
    this.excessComputeResourceRatio = excessComputeResourceRatio;
    this.numRequests = numRequests;
    this.numCpus = numCpus;
    this.seed = seed;
  }
}
