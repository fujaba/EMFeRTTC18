package de.tudresden.inf.st.mquat.generator;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ScenarioGenerator {

  // ranges for random variables
  private static final int cpu_freq_min = 500;
  private static final int cpu_freq_max = 3000;
  private static final int total_ram_min = 500;
  private static final int total_ram_max = 16000;
  private static final int total_disk_min = 500;
  private static final int total_disk_max = 16000;
  private static final int latency_min = 1;
  private static final int latency_max = 1000;
  private static final int throughput_min = 10;
  private static final int throughput_max = 100000;
  private static final int request_size_min = 1;
  private static final int request_size_max = 1000;
  private static final int quality_min = 0;
  private static final int quality_max = 100;
  private final ScenarioDescription description;
  private final Logger logger;
  private Solution initialSolution;
  private Random random;
  private int resourceCounter = 0;

  /**
   * A class to generate scalable scenarios for the Optaplanner domain
   *
   * @param description the configuration
   */
  public ScenarioGenerator(ScenarioDescription description) {
    this.description = description;
    this.logger = LogManager.getLogger(ScenarioGenerator.class);
    random = new Random();
  }

  public Solution getInitialSolution() {
    return initialSolution;
  }

//  private double getAverageComponentTreeSize() {
//    // (N^L-1) / (N-1)
//    return (Math.pow(getAvgNumSubComponents(), getComponentDepth()) - 1) / (getAvgNumSubComponents() - 1);
//  }

  private int nextRandomInt(int from, int to) {
    return from + random.nextInt(to - from + 1);
  }

  private int nextRandomGaussianInt(double mean, double derivation) {
    if (Math.round(derivation) == 0) {
      return (int) Math.round(mean);
    }
    double result;
    do {
      result = random.nextGaussian() * derivation + mean;
    } while (result < 0);
    return (int) Math.round(result);
  }

  public int getNumTopLevelComponents() {
    return description.numTopLevelComponents;
  }

  public int getAvgNumImplSubComponents() {
    return description.avgNumImplSubComponents;
  }

  public int getImplSubComponentStdDerivation() {
    return description.implSubComponentStdDerivation;
  }

  public int getAvgNumCompSubComponents() {
    return description.avgNumCompSubComponents;
  }

  public int getCompSubComponentStdDerivation() {
    return description.compSubComponentStdDerivation;
  }

  public int getNumImplementations() {
    return description.numImplementations;
  }

//  public int getNumModes() {
//    return description.numModes;
//  }

  public double getExcessResourceRatio() {
    return description.excessComputeResourceRatio;
  }

  public int getNumRequests() {
    return description.numRequests;
  }

  public int getComponentDepth() {
    return description.componentDepth;
  }

  public int getNumCpus() {
    return description.numCpus;
  }

  public long getSeed() {
    return description.seed;
  }

  public void printInfo() {
//    logger.info("Scenario ID:                                 {}", getId());
    logger.info("Number of top-level components:              {}", getNumTopLevelComponents());
    logger.info("Average number of impl sub-components:       {}", getAvgNumImplSubComponents());
    logger.info("Standard derivation of impl sub-components:  {}", getImplSubComponentStdDerivation());
    logger.info("Average number of comp sub-components:       {}", getAvgNumCompSubComponents());
    logger.info("Standard derivation of comp sub-components:  {}", getCompSubComponentStdDerivation());
    logger.info("Component tree depth:                        {}", getComponentDepth());
    logger.info("Number of implementations per component:     {}", getNumImplementations());
    logger.info("Excess compute resource ratio:               {}", getExcessResourceRatio());
    logger.info("Number cpus per compute resource:            {}", getNumCpus());
//    logger.info("Minimum number of required resources:        {}", getAverageComponentTreeSize() * getNumRequests());
    logger.info("Number of requests:                          {}", getNumRequests());
    logger.info("Seed:                                        {}", getSeed());
//    logger.info("Total number of configurations:              {}", getAverageComponentTreeSize() * getNumImplementations() * getNumModes() * getNumTopLevelComponents());
  }

  /**
   * generates the Jastadd model with the given seed of the object
   *
   * @return a Jastadd model with one guaranteed solution
   */
  public Root generate() {

    // reset the random seed to ensure reproducibility
    random.setSeed(getSeed());

    // generate the overall structure of the model
    Root root = new Root();
    HardwareModel hardwareModel = Root.createSimpleHardwareModel();
    root.setHardwareModel(hardwareModel);
    SoftwareModel softwareModel = new SoftwareModel();
    root.setSoftwareModel(softwareModel);

    // create the meta parameters
    MetaParameter sizeMetaParameter = new MetaParameter(new Name("size"));
    softwareModel.addMetaParameter(sizeMetaParameter);

//    Property[] nfp = new Property[getNumNfp()];
//    softwareModel.setPropertyList(new List<>());
//    for (int i=0; i < getNumNfp(); i++) {
//      nfp[i] = new Property(new Name("nfp" + String.valueOf(i)), "", PropertyKind.DERIVED);
//      softwareModel.addProperty(nfp[i]);
//    }

    // add two nonfunctional properties energy and quality
    Property energyProperty = new Property(new Name("energy"), "J");
    Property qualityProperty = new Property(new Name("quality"), "%");
    softwareModel.addProperty(energyProperty);
    softwareModel.addProperty(qualityProperty);

    // create the top-level components
    Component[] components = new Component[getNumTopLevelComponents()];
    for (int currentTopLevelComponent = 0; currentTopLevelComponent < getNumTopLevelComponents(); currentTopLevelComponent++) {
      Component component = createComponent(root, String.valueOf(currentTopLevelComponent), getComponentDepth());
      components[currentTopLevelComponent] = component;
    }

    // create the requests
    for (int i = 0; i < getNumRequests(); i++) {
      int requestSize = nextRandomInt(request_size_min, request_size_max);
      Request request = new Request();
      request.addMetaParameterAssignment(new MetaParameterAssignment(sizeMetaParameter.createRef(), new LiteralExpression(requestSize)));
      request.setTarget(components[i % getNumTopLevelComponents()].createRef());

      // add some constraints
      Clause qualityClause = new Clause(
          ClauseType.REQUIRING,
          new SoftwareDesignator(
              new Opt<>(),
              qualityProperty.createRef()
          ),
          ClauseComparator.GE,
          new LiteralExpression(nextRandomInt(quality_min, quality_max))
      );
      request.addConstraint(qualityClause);

      root.addRequest(request);

    }

    Objective objective = new Objective(energyProperty.createRef(), PropertyAggregation.SUM);
    root.setObjective(objective);

    // we need to make sure the rewrites do not result in faulty caches! HELP US, RACR!
    root.flushTreeCache();
    this.initialSolution = sanitize(root);

    root.flushTreeCache();

    // generate the excess resources
    if (getExcessResourceRatio() < 1) {
      throw new RuntimeException("Cannot generate a model with less resources than are required by the guaranteed solution!");
    }
    long totalResources = Math.round(getExcessResourceRatio() * hardwareModel.getNumResource());
    for (long i = hardwareModel.getNumResource() + 1; i <= totalResources; i++) {
      hardwareModel.addResource(createResource(root));
    }

    // shuffle the resources
    java.util.List<Resource> resourceJavaList = new ArrayList<>(root.getHardwareModel().getResourceList().asJavaCollection());
    root.getHardwareModel().setResourceList(new List<>());
    Collections.shuffle(resourceJavaList, random);
    for (Resource resource : resourceJavaList) {
      root.getHardwareModel().addResource(resource);
    }
    root.flushTreeCache();

    return root;
  }

  private Resource createResource(Root model) {
    int i = resourceCounter;
    resourceCounter++;

    HardwareModel hardwareModel = model.getHardwareModel();

    ResourceType type = hardwareModel.computeResourceType();
    ResourceType cpuType = hardwareModel.cpuType();
    ResourceType ramType = hardwareModel.ramType();
    ResourceType diskType = hardwareModel.diskType();
    ResourceType networkType = hardwareModel.networkType();

    Property frequency = cpuType.resolveProperty("frequency").get();
    Property load = cpuType.resolveProperty("load").get();
    Property total = ramType.resolveProperty("total").get();
    Property free = ramType.resolveProperty("free").get();
    Property throughput = networkType.resolveProperty("throughput").get();
    Property latency = networkType.resolveProperty("latency").get();

    // create a new resource
    Resource resource = new Resource(new Name("resource" + String.valueOf(i)), type.createRef(), new List<>(), new List<>());
    // add subresources
    // there are multiple cpus
    for (int numCpu = 0; numCpu < getNumCpus(); numCpu++) {
      Resource cpu1 = new Resource(new Name("cpu" + String.valueOf(i) + "_" + String.valueOf(numCpu)), cpuType.createRef(), new List<>(), new List<>());
      int currentFrequency = nextRandomInt(cpu_freq_min, cpu_freq_max);
      cpu1.addCurrentResourceValue(new CurrentResourceValue(frequency.createRef(), new LiteralExpression(currentFrequency)));
      cpu1.addCurrentResourceValue(new CurrentResourceValue(load.createRef(), new LiteralExpression(0)));
      resource.addSubResource(cpu1);
    }
    Resource ram1 = new Resource(new Name("ram" + String.valueOf(i)), ramType.createRef(), new List<>(), new List<>());
    int currentRam = nextRandomInt(total_ram_min, total_ram_max);
    ram1.addCurrentResourceValue(new CurrentResourceValue(total.createRef(), new LiteralExpression(currentRam)));
    ram1.addCurrentResourceValue(new CurrentResourceValue(free.createRef(), new LiteralExpression(currentRam)));
    resource.addSubResource(ram1);
    Resource disk1 = new Resource(new Name("disk" + String.valueOf(i)), diskType.createRef(), new List<>(), new List<>());
    int currentDisk = nextRandomInt(total_disk_min, total_disk_max);
    disk1.addCurrentResourceValue(new CurrentResourceValue(total.createRef(), new LiteralExpression(currentDisk)));
    disk1.addCurrentResourceValue(new CurrentResourceValue(free.createRef(), new LiteralExpression(currentDisk)));
    resource.addSubResource(disk1);
    Resource network1 = new Resource(new Name("network" + String.valueOf(i)), networkType.createRef(), new List<>(), new List<>());
    int currentThroughput = nextRandomInt(throughput_min, throughput_max);
    int currentLatency = nextRandomInt(latency_min, latency_max);
    network1.addCurrentResourceValue(new CurrentResourceValue(latency.createRef(), new LiteralExpression(currentLatency)));
    network1.addCurrentResourceValue(new CurrentResourceValue(throughput.createRef(), new LiteralExpression(currentThroughput)));
    resource.addSubResource(network1);
    return resource;
  }

  /**
   * creates a component and adds it to the given model.
   *
   * @param model       The model all (recursively) created components are added to
   * @param componentId the component id, something like "1_2_3"
   * @param depth       the depth of the component subtree
   * @return the directy created component. This is required for dependency tracking in recursive calls.
   */
  private Component createComponent(Root model, String componentId, int depth) {
    Component component = new Component();
    component.setName(new Name("component_" + componentId));
    model.getSoftwareModel().addComponent(component);

    // get the ResourceTypes we need
    ResourceType cpu = model.getHardwareModel().resolveResourceType("ComputeNode").get().resolveResourceType("CPU").get();
    ResourceType ram = model.getHardwareModel().resolveResourceType("ComputeNode").get().resolveResourceType("RAM").get();
    ResourceType disk = model.getHardwareModel().resolveResourceType("ComputeNode").get().resolveResourceType("DISK").get();
    ResourceType network = model.getHardwareModel().resolveResourceType("ComputeNode").get().resolveResourceType("NETWORK").get();

    Property frequency = cpu.resolveProperty("frequency").get();
    Property total = ram.resolveProperty("total").get();
    Property throughput = network.resolveProperty("throughput").get();
    Property quality = model.getSoftwareModel().resolveProperty("quality").get();
    Property energy = model.getSoftwareModel().resolveProperty("energy").get();
//    Property flops = model.getHardwareModel().getResourceType(0).getPropertyByName("flops");

    component.addPropertyRef(quality.createRef());
    component.addPropertyRef(energy.createRef());

    // create the component requirements
//    java.util.List<Clause> clauseList = new java.util.ArrayList<>();
//    java.util.List<ComponentRequirement> componentRequirementList = new java.util.ArrayList<>();
    java.util.List<Component> componentList = new java.util.ArrayList<>();

    // if the component has a depth above one, it has references to two sub-components
    if (depth > 1) {
      for (int currentSubComponent = 0; currentSubComponent < nextRandomGaussianInt(getAvgNumCompSubComponents(), getCompSubComponentStdDerivation());
           currentSubComponent++) {

        // recursive call
        Component subComponent = createComponent(model,
            componentId + "c" + currentSubComponent, depth - 1);

        componentList.add(subComponent);

//        // an instance is needed, otherwise no requirements can be properly written
//        Instance componentInstance = new Instance(new Name("the_" + subComponent.getName().getName()));
//
//        ComponentRequirement requirement = new ComponentRequirement();
//        requirement.setComponentRef(subComponent.createRef());
//        requirement.addInstance(componentInstance);
//
//        // add required quality clause to implementation
//        Clause qualityClause = new Clause();
//        qualityClause.setDesignator(new SoftwareDesignator(new Opt<>(componentInstance.createRef()), quality.createRef()));
//        qualityClause.setClauseType(ClauseType.REQUIRING);
//        qualityClause.setClauseComparator(ClauseComparator.GE);
//        int randQuality = nextRandomInt(quality_min, quality_max);
//        qualityClause.setExpression(new LiteralExpression(randQuality));
//        clauseList.add(qualityClause);
//
//        componentRequirementList.add(requirement);
      }
    }

    for (int currentImplementation = 0; currentImplementation < getNumImplementations(); currentImplementation++) {
      Implementation implementation = new Implementation();

      if (depth > 1) {
        for (int currentSubComponent = 0; currentSubComponent < nextRandomGaussianInt(getAvgNumCompSubComponents(), getCompSubComponentStdDerivation());
             currentSubComponent++) {
          Component subComponent = componentList.get(currentSubComponent);
          // an instance is needed, otherwise no requirements can be properly written
          Instance componentInstance = new Instance(new Name("the_" + subComponent.getName().getName()));

          ComponentRequirement requirement = new ComponentRequirement();
          requirement.setComponentRef(subComponent.createRef());
          requirement.addInstance(componentInstance);

          // add required quality clause to implementation
          Clause qualityClause = new Clause();
          qualityClause.setDesignator(new SoftwareDesignator(new Opt<>(componentInstance.createRef()), quality.createRef()));
          qualityClause.setClauseType(ClauseType.REQUIRING);
          qualityClause.setClauseComparator(ClauseComparator.GE);
          int randQuality = nextRandomInt(quality_min, quality_max);
          qualityClause.setExpression(new LiteralExpression(randQuality));
          implementation.addClause(qualityClause);

          implementation.addComponentRequirement(requirement);
        }
      }

      String implementationId = componentId + "i" + currentImplementation;
      implementation.setName(new Name("implementation_" + implementationId));


      // add the compute resource requirement
      ResourceRequirement computeReqirement = new ResourceRequirement();
      computeReqirement.setResourceTypeRef(model.getHardwareModel().getResourceType(0).createRef());
      Instance computeInstance = new Instance(new Name("compute_resource_0"));
      computeReqirement.addInstance(computeInstance);
      implementation.setResourceRequirement(computeReqirement);

      // there are multiple cpus possible
      Instance[] cpuInstances = new Instance[getNumCpus()];
      ResourceRequirement cpuRequirement = new ResourceRequirement();
      cpuRequirement.setResourceTypeRef(cpu.createRef());
      for (int numCpu = 0; numCpu < getNumCpus(); numCpu++) {
        Instance cpuInstance = new Instance(new Name("cpu_" + numCpu));
        cpuRequirement.addInstance(cpuInstance);
        cpuInstances[numCpu] = cpuInstance;
      }
      computeReqirement.addResourceRequirement(cpuRequirement);
      ResourceRequirement ramRequirement = new ResourceRequirement();
      ramRequirement.setResourceTypeRef(ram.createRef());
      Instance ram1 = new Instance(new Name("ram_1"));
      ramRequirement.addInstance(ram1);
      computeReqirement.addResourceRequirement(ramRequirement);
      ResourceRequirement diskRequirement = new ResourceRequirement();
      diskRequirement.setResourceTypeRef(disk.createRef());
      Instance disk1 = new Instance(new Name("disk_1"));
      diskRequirement.addInstance(disk1);
      computeReqirement.addResourceRequirement(diskRequirement);
      ResourceRequirement networkRequirement = new ResourceRequirement();
      networkRequirement.setResourceTypeRef(network.createRef());
      Instance network1 = new Instance(new Name("network_1"));
      networkRequirement.addInstance(network1);
      computeReqirement.addResourceRequirement(networkRequirement);

      component.addImplementation(implementation);

      // if the implementation has a depth above one, it has references to two sub-components
      if (depth > 1) {
        for (int currentSubComponent = 0; currentSubComponent < nextRandomGaussianInt(getAvgNumImplSubComponents(), getImplSubComponentStdDerivation());
             currentSubComponent++) {
          ComponentRequirement requirement = new ComponentRequirement();

          // recursive call
          Component subComponent = createComponent(model,
              componentId + "i" + currentImplementation + "_" + currentSubComponent, depth - 1);

          requirement.setComponentRef(subComponent.createRef());

          // an instance is needed, otherwise no requirements can be properly written
          Instance componentInstance = new Instance(new Name("the_" + subComponent.getName().getName()));
          requirement.addInstance(componentInstance);

          // add required quality clause to implementation
          Clause qualityClause = new Clause();
          qualityClause.setDesignator(new SoftwareDesignator(new Opt<>(componentInstance.createRef()), quality.createRef()));
          qualityClause.setClauseType(ClauseType.REQUIRING);
          qualityClause.setClauseComparator(ClauseComparator.GE);
          int randQuality = nextRandomInt(quality_min, quality_max);
          qualityClause.setExpression(new LiteralExpression(randQuality));
          implementation.addClause(qualityClause);

          implementation.addComponentRequirement(requirement);
        }
      }

      int randFreq = nextRandomInt(cpu_freq_min, cpu_freq_max);
      int randRam = nextRandomInt(total_ram_min, total_ram_max);
      int randDisk = nextRandomInt(total_disk_min, total_disk_max);
      int randThroughput = nextRandomInt(throughput_min, throughput_max);
      int randQuality = nextRandomInt(quality_min, quality_max);

      // add the requiring hardware clauses
      // there are multiple cpus possible
      for (int numCpu = 0; numCpu < getNumCpus(); numCpu++) {
        Clause cpuClause = new Clause(
            ClauseType.REQUIRING,
            new PropertyResourceDesignator(cpuInstances[numCpu].createRef(), frequency.createRef()),
            ClauseComparator.GE,
            new LiteralExpression(randFreq)
        );
        implementation.addClause(cpuClause);
      }
      Clause ramClause = new Clause(
          ClauseType.REQUIRING,
          new PropertyResourceDesignator(ram1.createRef(), total.createRef()),
          ClauseComparator.GE,
          new LiteralExpression(randRam)
      );
      implementation.addClause(ramClause);
      Clause diskClause = new Clause(
          ClauseType.REQUIRING,
          new PropertyResourceDesignator(disk1.createRef(), total.createRef()),
          ClauseComparator.GE, new LiteralExpression(randDisk)
      );
      implementation.addClause(diskClause);
      Clause networkClause = new Clause(
          ClauseType.REQUIRING,
          new PropertyResourceDesignator(network1.createRef(), throughput.createRef()),
          ClauseComparator.GE,
          new LiteralExpression(randThroughput)
      );
      implementation.addClause(networkClause);

      // add flops provision clause

      // build an expression adding all cpu frequencies
      Expression cpuExpression = new PropertyResourceDesignator(cpuInstances[0].createRef(), frequency.createRef());
      for (int currentCpu = 1; currentCpu < getNumCpus(); currentCpu++) {
        cpuExpression = new AddExpression(
            cpuExpression,
            new PropertyResourceDesignator(cpuInstances[currentCpu].createRef(), frequency.createRef())
        );
      }


//        Clause flopsClause = new Clause(
//            ClauseType.PROVIDING,
//            new SoftwareDesignator(new Opt<>(), flops.createRef()),
//            // new PropertyResourceDesignator(computeInstance.createRef(), flops.createRef()),
//            ClauseComparator.EQ,
//            cpuExpression
//        );
//        configuration.addClause(flopsClause);

      // add the providing software clauses
      Clause qualityClause = new Clause(
          ClauseType.PROVIDING,
          new SoftwareDesignator(new Opt<>(), quality.createRef()),
          ClauseComparator.EQ,
          new LiteralExpression(randQuality)
      );
      implementation.addClause(qualityClause);
      // return 0.1*size^2 - 0.3*cpu.frequency
      double factor1 = Math.round(random.nextDouble() * 100) / 100d;
      double factor2 = Math.round(random.nextDouble() * 100) / 100d;
      MetaParameterRef sizeRef = model.getSoftwareModel().getMetaParameter(0).createRef();
      Clause energyClause = new Clause(
          ClauseType.PROVIDING,
          new SoftwareDesignator(new Opt<>(), energy.createRef()),
          ClauseComparator.EQ,
          new AddExpression(
              new MultExpression(
                  new LiteralExpression(factor1),
                  new PowerExpression(
                      new MetaParameterDesignator(sizeRef),
                      new LiteralExpression(2)
                  )
              ),
              new MultExpression(
                  new LiteralExpression(factor2),
                  cpuExpression
              )
          )
      );
      implementation.addClause(energyClause);

    }

    return component;
  }

  private Solution sanitize(Root model) {
    Solution solution = new Solution();
    solution.setModel(model);

    for (Request request : model.getRequests()) {

      // get the size of the request
      int quality = (int) request.getConstraintValueByName("quality");

      // fix the tree for the given request
      sanitize(request, request.getTarget().getRef(), quality, solution, true);
    }

    return solution;
  }

  private Assignment sanitize(Request request, Component component, int quality, Solution solution, boolean isTopLevel) {

    Root model = solution.getModel();

    ResourceType computeType = model.getHardwareModel().getResourceType(0);
    ResourceType cpuType = model.getHardwareModel().getResourceType(0).resolveResourceType("CPU").get();
    ResourceType ramType = model.getHardwareModel().getResourceType(0).resolveResourceType("RAM").get();
    ResourceType diskType = model.getHardwareModel().getResourceType(0).resolveResourceType("DISK").get();
    ResourceType networkType = model.getHardwareModel().getResourceType(0).resolveResourceType("NETWORK").get();

    Assignment currentAssignment = new Assignment();
    currentAssignment.setTopLevel(isTopLevel);
    currentAssignment.setRequest(request);

    // pick an arbitrary implementation
    Implementation implementation = component.getImplementation(random.nextInt(component.getNumImplementation()));
    currentAssignment.setImplementation(implementation);


    // add a new resource to allocate the current component to
    Resource resource = createResource(model);
    model.getHardwareModel().addResource(resource);
    currentAssignment.setResourceMapping(new ResourceMapping(currentAssignment.getImplementation().getResourceRequirement().getInstance(0), resource, new List<>()));

    // do the recursive assignment of dependent components
    for (ComponentRequirement componentRequirement : implementation.getComponentRequirementList()) {
      for (Instance requiredInstance : componentRequirement.getInstanceList()) {
        int newQualityValue = 0;

        // find the clause in the implementation that references the instance get the quality from it
        for (Clause clause : implementation.getClauseList()) {
          if (clause.isRequiringClause()) {
            if (clause.getDesignator().isSoftwareDesignator()) {
              if (clause.getDesignator().asSoftwareDesignator().getInstanceRef().getRef() == requiredInstance) {
                newQualityValue = (int) clause.getExpression().evalAsDouble();
              }
            }
          }
        }
        Assignment assignmentReference = sanitize(request, componentRequirement.getComponentRef().getRef(), newQualityValue, solution, false);
        currentAssignment.addComponentMapping(new ComponentMapping(requiredInstance, assignmentReference));
      }
    }

    // set the provided quality of the configuration
    for (Clause clause : implementation.getClauseList()) {
      if (clause.isProvidingClause()
          && clause.getDesignator().simpleName().equals("quality")) {
        if (logger.isTraceEnabled()) {
          logger.trace("set quality value from {} to {}", clause.getExpression().evalAsDouble(), quality);
        }
        clause.setExpression(new LiteralExpression(Math.max((int) clause.getExpression().evalAsDouble(), quality)));
      }
    }

    // get the hardware requirements
    Instance computeInstance = null;
    ResourceRequirement rr = implementation.getResourceRequirement();
    if (rr.getResourceTypeRef().getRef().equals(computeType)) {
      computeInstance = rr.getInstance(0);
    }


    int frequencyValue[] = new int[getNumCpus()];
    Instance cpuInstance[] = new Instance[getNumCpus()];
    for (int currentCPU = 0; currentCPU < getNumCpus(); currentCPU++) {
      frequencyValue[currentCPU] = (int) implementation.getRequiringClauseValue(cpuType, "frequency") + 1;
      cpuInstance[currentCPU] = implementation.getRequiringClauseInstance(cpuType, "frequency", currentCPU);
    }

    int totalRamValue = (int) implementation.getRequiringClauseValue(ramType, "total") + 1;
    Instance ramInstance = implementation.getRequiringClauseInstance(ramType, "total");
    int totalDiskValue = (int) implementation.getRequiringClauseValue(diskType, "total") + 1;
    Instance diskInstance = implementation.getRequiringClauseInstance(diskType, "total");
    int networkThroughputValue = (int) implementation.getRequiringClauseValue(networkType, "throughput") + 1;
    Instance networkInstance = implementation.getRequiringClauseInstance(networkType, "throughput");

    // put the compute resource into the assignment
    ResourceMapping computeResourceMapping = new ResourceMapping();
    computeResourceMapping.setInstance(computeInstance);
    computeResourceMapping.setResource(resource);
    currentAssignment.setResourceMapping(computeResourceMapping);

    // set the values
    int currentCpu = 0;
    for (Resource sub : resource.getSubResourceList()) {
      if (sub.getType().getRef().equals(cpuType)) {
        for (CurrentResourceValue value : sub.getCurrentResourceValueList()) {
          if (value.getPropertyRef().getRef().getName().getName().equals("frequency")) {
            computeResourceMapping.addResourceMapping(new ResourceMapping(cpuInstance[currentCpu], sub, new List<>()));
            if (logger.isTraceEnabled()) {
              logger.trace("set frequency value from {} to {}", value.getValue().evalAsDouble(), frequencyValue[currentCpu]);
            }
            value.setValue(new LiteralExpression(frequencyValue[currentCpu]));
            currentCpu++;
          }
        }
      } else if (sub.getType().getRef().equals(ramType)) {
        setTotalStorageValue(computeResourceMapping, totalRamValue, ramInstance, sub);
        computeResourceMapping.addResourceMapping(new ResourceMapping(ramInstance, sub, new List<>()));
      } else if (sub.getType().getRef().equals(diskType)) {
        setTotalStorageValue(computeResourceMapping, totalDiskValue, diskInstance, sub);
        computeResourceMapping.addResourceMapping(new ResourceMapping(diskInstance, sub, new List<>()));
      } else if (sub.getType().getRef().equals(networkType)) {
        for (CurrentResourceValue value : sub.getCurrentResourceValueList()) {
          if (value.getPropertyRef().getRef().getName().getName().equals("throughput")) {
            computeResourceMapping.addResourceMapping(new ResourceMapping(networkInstance, sub, new List<>()));
            if (logger.isTraceEnabled()) {
              logger.trace("set throughput value from {} to {}", value.getValue().evalAsDouble(), networkThroughputValue);
            }
            value.setValue(new LiteralExpression(networkThroughputValue));
          }
        }
      }
    }

    // finally, add the current assignment if it is a top-level assignment
    if (isTopLevel) {
      solution.addAssignment(currentAssignment);
    }
    return currentAssignment;

  }

  private void setTotalStorageValue(ResourceMapping computeResourceMapping, int totalStorageValue, Instance storageInstance, Resource sub) {
    for (CurrentResourceValue value : sub.getCurrentResourceValueList()) {
      String resourceName = value.getPropertyRef().getRef().getName().getName();
      if (resourceName.equals("total") || resourceName.equals("free")) {
        value.setValue(new LiteralExpression(totalStorageValue));
      }
    }
  }


}
