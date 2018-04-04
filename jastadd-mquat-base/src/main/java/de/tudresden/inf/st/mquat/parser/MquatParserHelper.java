package de.tudresden.inf.st.mquat.parser;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolving names while parsing model and solution files.
 *
 * @author rschoene - Initial contribution
 */
public class MquatParserHelper {

  private Logger logger = LogManager.getLogger(MquatParserHelper.class);
  // create new fields for references
  public Map<String, Instance> instanceMap = new HashMap<>();
  public Map<String, Component> componentMap = new HashMap<>();
  public Map<String, Property> propertyMap = new HashMap<>();
  public Map<String, ResourceType> resourceTypeMap = new HashMap<>();
  public Map<String, MetaParameter> metaParameterMap = new HashMap<>();
  public java.util.List<InstanceRef> instanceRefList = new ArrayList<>();
  public java.util.List<ComponentRef> componentRefList = new ArrayList<>();
  public java.util.List<PropertyRef> propertyRefList = new ArrayList<>();
  public java.util.List<ResourceTypeRef> resourceTypeRefList = new ArrayList<>();
  public java.util.List<MetaParameterRef> metaParameterRefList = new ArrayList<>();

  // references to be resolved for solution
  // assignment -> (requestName|instanceName, implName)
  public Map<Assignment, Tuple<String, String>> assignmentTerminals = new HashMap<>();
  // resource mapping -> (instanceName, resourceName)
  public Map<ResourceMapping, Tuple<String, String>> resourceMappingTerminals = new HashMap<>();
  public Solution unfinishedSolution;

  interface RefToName<RefType> {
    String getName(RefType ref);
  }
  interface SetRef<RefType, RealType> {
    void setRef(RefType ref, RealType resolved);
  }

  private <RefType, RealType> void resolve(
      java.util.List<RefType> refList,
      Map<String, RealType> refMap,
      RefToName<RefType> rtn,
      SetRef<RefType, RealType> sr) {
    for (RefType ref : refList) {
      RealType resolved = refMap.get(rtn.getName(ref));
      if (resolved == null) {
        logger.warn("reference in {} '{}' cannot be resolved",
            ref.getClass().getSimpleName(), rtn.getName(ref));
      }
      sr.setRef(ref, resolved);
    }
  }

  /**
   * Post processing step after parsing a model, to resolve all references within the model.
   * @throws java.util.NoSuchElementException if a reference can not be resolved
   */
  public void resolveReferences() {
    resolve(instanceRefList, instanceMap, ref -> ref.getName().getName(), InstanceRef::setRef);
    resolve(componentRefList, componentMap, ref -> ref.getName().getName(), ComponentRef::setRef);
    resolve(propertyRefList, propertyMap, ref -> ref.getName().getName(), PropertyRef::setRef);
    resolve(resourceTypeRefList, resourceTypeMap, ref -> ref.getName().getName(), ResourceTypeRef::setRef);
    resolve(metaParameterRefList, metaParameterMap, ref -> ref.getName().getName(), MetaParameterRef::setRef);
  }

  private void resolveResourceMappingOf(Assignment assignment) {
    ResourceMapping rm = assignment.getResourceMapping();
    Implementation impl = assignment.getImplementation();
    Tuple<String, String> tuple = resourceMappingTerminals.get(rm);
    // first name in tuple is instance name
    // resolve instance using implementation of assignment
    Instance instance = impl.findInstanceByName(tuple.getFirstElement());
    rm.setInstance(instance);
    // second name in tuple is resource name
    // resolve top-level resource using model
    Resource container = impl.root().findResourceByName(tuple.getSecondElement());
    rm.setResource(container);
    ResourceRequirement rr = instance.containingResourceRequirement();
    for (ResourceMapping subResMapping : rm.getResourceMappingList()) {
      resolveSubResourceMapping(subResMapping, container, rr);
    }
  }

  private void resolveSubResourceMapping(ResourceMapping rm, Resource container, ResourceRequirement rr) {
    // resolve sub-resource using the top-level resource, and the corresponding resource requirement
    Tuple<String, String> tuple = resourceMappingTerminals.get(rm);
    // first name in tuple is instance name
    Instance instance = rr.findInstanceByName(tuple.getFirstElement());
    rm.setInstance(instance);
    // second name in tuple is resource name
    Resource resource = container.findResourceByName(tuple.getSecondElement());
    rm.setResource(resource);
    if (rm.getNumResourceMapping() > 0) {
      ResourceRequirement subResReq = instance.containingResourceRequirement();
      for (ResourceMapping subResMapping : rm.getResourceMappingList()) {
        resolveSubResourceMapping(subResMapping, resource, subResReq);
      }
    }
  }

  /**
   * Post processing step after parsing a solution, to resolve all references.
   * @param model the model to resolve the references
   * @throws RuntimeException if assignments are malformed
   * @throws java.util.NoSuchElementException if a reference can not be resolved
   */
  public void resolveSolutionReferencesWith(Root model) {
    this.unfinishedSolution.setModel(model);
    // first set request names for all top-level assignments
    for (Assignment assignment : unfinishedSolution.getAssignmentList()) {
      Tuple<String, String> value = assignmentTerminals.get(assignment);
      // first name in value is request name
      Request request = model.findRequestByName(value.getFirstElement());
      assignment.setRequest(request);
      // second name in value is impl name
      Implementation impl = model.findImplementationByName(value.getSecondElement());
      assignment.setImplementation(impl);
      resolveResourceMappingOf(assignment);
    }
    // now iterate over nested assignments, i.e., non-top-level
    for (Map.Entry<Assignment, Tuple<String, String>> entry : assignmentTerminals.entrySet()) {
      Assignment assignment = entry.getKey();
      if (assignment.getTopLevel()) {
        continue;
      }
      Request request = null;
      Assignment parentAssignment = assignment;  // start with this assignment
      java.util.List<Assignment> assignmentsWithoutRequest = new java.util.ArrayList<>();
      assignmentsWithoutRequest.add(assignment);
      while (request == null) {
        ComponentMapping cm = parentAssignment.containingComponentMapping();
        if (cm == null) {
          throw new RuntimeException("Assignment has no parent");
        }
        parentAssignment = cm.containingAssignment();
        if (parentAssignment == null) {
          // should never happen
          throw new RuntimeException("ComponentMapping has no parent");
        }
        request = parentAssignment.getRequest();
        if (request == null) {
          assignmentsWithoutRequest.add(parentAssignment);
        }
      }
      // set request for all assignments found "on the way up"
      for (Assignment assignmentWithoutRequest : assignmentsWithoutRequest) {
        assignmentWithoutRequest.setRequest(request);
      }
      // first name in value is name of instance, set it for component mapping
      ComponentMapping cm = assignment.containingComponentMapping();
      // to find correct instance, we need to start at implementation of parentAssignment
      parentAssignment = cm.containingAssignment();
      cm.setInstance(parentAssignment.getImplementation().findInstanceByName(entry.getValue().getFirstElement()));
      // second name in value is name of impl
      Implementation impl = model.findImplementationByName(entry.getValue().getSecondElement());
      assignment.setImplementation(impl);
      resolveResourceMappingOf(assignment);
    }
  }

}
