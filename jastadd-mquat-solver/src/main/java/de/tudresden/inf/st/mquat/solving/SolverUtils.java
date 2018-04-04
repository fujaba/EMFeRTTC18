package de.tudresden.inf.st.mquat.solving;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
public class SolverUtils {

  public static void populateResourceMapping(ResourceMapping mapping, ResourceRequirement requirement, Resource resource) {

    for (ResourceRequirement subRequirement : requirement.getResourceRequirementList()) {
      int fittingResourceCount = 0;
      for (int currentInstance = 0; currentInstance < subRequirement.getNumInstance(); currentInstance++) {
        Instance instance = subRequirement.getInstance(currentInstance);
        for (int currentResource = 0; currentResource < resource.getNumSubResource(); currentResource++) {
          Resource subResource = resource.getSubResource(currentResource);
          if (subResource.getType().getRef() == subRequirement.getResourceTypeRef().getRef()) {
            if (currentInstance == fittingResourceCount) {
              ResourceMapping newMapping = new ResourceMapping(instance, subResource, new de.tudresden.inf.st.mquat.jastadd.model.List<>());
              mapping.addResourceMapping(newMapping);
              populateResourceMapping(newMapping, subRequirement, subResource);
              fittingResourceCount++;
            }
            currentInstance++;
          }
        }
      }
    }
  }

  public static long populateSolution(List<Assignment> listOfInitialAssignments, Solution result, Logger logger)
      throws SolvingException {
    StopWatch watch = StopWatch.start();
    Map<Tuple<Request, Component>, Assignment> tupleAssignmentMap = new HashMap<>();

    for (Assignment assignment : listOfInitialAssignments) {
      Implementation impl = assignment.getImplementation();
      Resource resource = assignment.getResource();

      ResourceMapping mapping = new ResourceMapping(impl.getResourceRequirement().getInstance(0), resource, new de.tudresden.inf.st.mquat.jastadd.model.List<>());
      populateResourceMapping(mapping, impl.getResourceRequirement(), resource);
      assignment.setResourceMapping(mapping);

//      for (ResourceRequirement rr : impl.getResourceRequirementList()) {
//        ResourceType requiredType = rr.getResourceTypeRef().getRef();
//        if (requiredType.equals(resource.getType().getRef())) {
//          // computeNode
//          assignment.addResourceMapping(new ResourceMapping(rr.getInstance(0), resource));
//        } else {
//          // cpu, ram, disk, network
//          for (int i = 0; i < rr.getNumInstance(); i++) {
//            // find in resource the i-th sub-resource with matching type
//            int remaining = i + 1;
//            int subI;
//            for (subI = 0; subI < resource.getNumSubResource(); subI++) {
//              if (resource.getSubResource(subI).getType().getRef().equals(requiredType)) {
//                if (--remaining == 0) {
//                  break;
//                }
//              }
//            }
//            if (remaining > 0) {
//              throw new SolvingException("Could not find the " + (i + 1) + "-th sub-resource in " +
//                  resource.getIlpName() +
//                  " with type " + requiredType + ". Only found " + (i + 1 - remaining));
//            }
//            Resource subResource = resource.getSubResource(subI);
//            if (subResource == null) {
//              throw new SolvingException("Could not find the " + (i + 1) + "-th sub-resource in " +
//                  resource.getIlpName() +
//                  " with type " + requiredType.getIlpName() + ". Tried with " + (subI + 1) + "-th one.");
//            }
//            assignment.addResourceMapping(new ResourceMapping(rr.getInstance(i), subResource));
//          }
//        }
//      }
      tupleAssignmentMap.put(Tuple.of(assignment.getRequest(), impl.containingComponent()), assignment);
    }

    // set componentRequirementAssignments and add assignments to result
    // assignments are not final upon adding, thus should not be validated before loop exits
    for (Assignment assignment : tupleAssignmentMap.values()) {
      Implementation impl = assignment.getImplementation();
      for (ComponentRequirement cr : impl.getComponentRequirementList()) {
        Component requiredComponent = cr.getComponentRef().getRef();
        Assignment providingAssignment = tupleAssignmentMap.get(Tuple.of(assignment.getRequest(), requiredComponent));
        if (providingAssignment == null) {
          logger.warn("No assignment found for component {} at {} required in {}",
              requiredComponent.getIlpName(), assignment.getRequest().getIlpName(), impl.getIlpName());
          continue;
        }
        assignment.addComponentMapping(new ComponentMapping(cr.getInstance(0), providingAssignment));
        if (cr.getNumInstance() > 1) {
          logger.warn("Can not handle more than one required instance for {} in impl {}. Skipping all but first.",
              requiredComponent.getIlpName(), impl.getIlpName());
        }
      }

      if (impl.containingComponent().equals(assignment.getRequest().getTarget().getRef())) {
        assignment.setTopLevel(true);
        result.addAssignment(assignment);
      } else {
        assignment.setTopLevel(false);
      }
    }
    long solutionCreation = watch.time(TimeUnit.MILLISECONDS);
    logger.debug("Solution creation took {}ms.", solutionCreation);
    return solutionCreation;

  }
}
