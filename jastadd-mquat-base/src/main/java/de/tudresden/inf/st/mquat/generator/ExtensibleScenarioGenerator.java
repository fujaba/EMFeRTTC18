package de.tudresden.inf.st.mquat.generator;

import de.tudresden.inf.st.mquat.jastadd.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Generator for scenarios delegating the work to {@link ScenarioGenerator} and using a {@link ModelSerializer} to build custom models.
 *
 * @author rschoene - Initial contribution
 */
public class ExtensibleScenarioGenerator {

  private ModelSerializer serializer;
  private ScenarioGenerator delegatee;

  public ExtensibleScenarioGenerator() {
    // empty
  }

  public ExtensibleScenarioGenerator(ModelSerializer serializer, ScenarioDescription description) {
    setSerializer(serializer);
    setDescription(description);
  }

  public void setSerializer(ModelSerializer serializer) {
    this.serializer = serializer;
  }

  public void setDescription(ScenarioDescription description) {
    this.delegatee = new ScenarioGenerator(description);
  }

  public void generateModel() throws Exception {
    Objects.requireNonNull(serializer, "Serializer must be set!");
    Objects.requireNonNull(delegatee, "Description must be set!");
    serializer.initModel();
    constructModel();
    serializer.persistModel();
  }

  private void constructModel() {
    Root model = delegatee.generate();
    Map<ASTNode, Object> vertices = new HashMap<>();

    // hwmodel
    HardwareModel hwModel = model.getHardwareModel();
    for (Property property : hwModel.getPropertyList()) {
      vertices.put(property, serializer.createProperty(property));
    }

    for (ResourceType resourceType : hwModel.getResourceTypeList()) {
      serializeResourceType(resourceType, vertices);
    }

    for (Resource resource : hwModel.getResourceList()) {
      serializeResource(resource, vertices);
    }
    hwModel = null;

    // swmodel
    SoftwareModel swModel = model.getSoftwareModel();
    for (MetaParameter mp : swModel.getMetaParameterList()) {
      vertices.put(mp, serializer.createMetaParameter(mp));
    }

    for (Property property : swModel.getPropertyList()) {
      vertices.put(property, serializer.createProperty(property));
    }

    for (Component component : swModel.getComponentList()) {
      Object serializedComponent = serializer.createComponent(component);
      vertices.put(component, serializedComponent);
      for (Property property : component.getPropertyList()) {
        vertices.put(property, serializer.createProperty(property));
      }
      for (PropertyRef propertyRef : component.getPropertyRefList()) {
        serializer.createEdge("PropertyRef", serializedComponent, vertices.get(propertyRef.getRef()));
      }
    }

    for (Implementation impl : model.allImplementations()) {
      vertices.put(impl, serializer.createImplementation(impl));
      for (ComponentRequirement cRequirement : impl.getComponentRequirementList()) {
        Object serializedReq = serializer.createComponentRequirement();
        serializer.createEdge("Component", serializedReq, vertices.get(cRequirement.getComponentRef().getRef()));
        for (Instance instance : cRequirement.getInstanceList()) {
          serializer.createEdge("Instance", serializedReq, serializer.createInstance(instance));
        }
      }
      ResourceRequirement rRequirement = impl.getResourceRequirement();
      Object serializedReq = serializer.createResourceRequirement();
      serializer.createEdge("ResourceType", serializedReq, vertices.get(rRequirement.getResourceTypeRef().getRef()));
      for (Instance instance : rRequirement.getInstanceList()) {
        serializer.createEdge("Instance", serializedReq, serializer.createInstance(instance));
      }
      for (Clause clause : impl.getClauseList()) {
        serializeClause(clause, vertices);
      }
    }

    // requests
    for (Request request : model.getRequestList()) {
      Object serializedRequest = serializer.createRequest(request);
      for (MetaParameterAssignment mpa : request.getMetaParameterAssignmentList()) {
        serializer.createEdge("MetaParameter", serializer.createMetaParameterAssignment(), vertices.get(mpa.getMetaParameterRef().getRef()));
      }
      serializer.createEdge("Target", serializedRequest, vertices.get(request.getTarget().getRef()));
      for (Clause clause : request.getConstraintList()) {
        serializeClause(clause, vertices);
      }
    }

    serializer.createEdge("Property", serializer.createObjective(model.getObjective()), model.getObjective().getPropertyRef().getRef());
  }

  private void serializeClause(Clause clause, Map<ASTNode, Object> vertices) {
    Object serializedClause = serializer.createClause(clause);
    serializer.createEdge("Designator", serializedClause, serializeDesignator(clause.getDesignator(), vertices));
    serializer.createEdge("Expression", serializedClause, serializeExpression(clause.getExpression(), vertices));
//    return serializedClause;
  }

  private Object serializeDesignator(Designator designator, Map<ASTNode, Object> vertices) {
    Object serializedDesignator = serializer.createDesignator(designator);

    // special handling for subclass
    if (designator.isSoftwareDesignator()) {
      SoftwareDesignator softwareDesignator = (SoftwareDesignator) designator;
      serializer.createEdge("Property", serializedDesignator, vertices.get(softwareDesignator.getPropertyRef()));
      if (softwareDesignator.hasInstanceRef()) {
        serializer.createEdge("Instance", serializedDesignator, vertices.get(softwareDesignator.getInstanceRef().getRef()));
      }
    }

    if (designator instanceof PropertyResourceDesignator) {
      PropertyResourceDesignator propertyResourceDesignator = (PropertyResourceDesignator) designator;
      serializer.createEdge("Property", serializedDesignator, vertices.get(propertyResourceDesignator.getPropertyRef()));
      serializer.createEdge("Instance", serializedDesignator, vertices.get(propertyResourceDesignator.getInstanceRef().getRef()));
    }

    if (designator instanceof MetaParameterDesignator) {
      serializer.createEdge("MetaParameter", serializedDesignator, vertices.get(((MetaParameterDesignator) designator).getMetaParameterRef().getRef()));
    }

    return serializedDesignator;
  }

  private Object serializeExpression(Expression expression, Map<ASTNode, Object> vertices) {
    if (expression instanceof BinaryExpression) {
      Object serializedExpression = serializer.createExpression(expression);
      BinaryExpression binaryExpression = (BinaryExpression) expression;
      Object left = serializer.createExpression(binaryExpression.getLeft());
      Object right = serializer.createExpression(binaryExpression.getRight());
      serializer.createEdge("Left", serializedExpression, left);
      serializer.createEdge("Right", serializedExpression, right);
      return serializedExpression;
    } else if (expression instanceof Designator){
      return serializeDesignator((Designator) expression, vertices);
    } else if (expression instanceof LiteralExpression) {
      return serializer.createLiteralExpression((LiteralExpression) expression);
    } else {
      throw new UnsupportedOperationException("Can not serialize " + expression);
    }
  }

  private void serializeResourceType(ResourceType resourceType, Map<ASTNode, Object> vertices) {
    Object serializedResourceType = serializer.createResourceType(resourceType);
    vertices.put(resourceType, serializedResourceType);
    for (Property property : resourceType.getPropertyList()) {
      vertices.put(property, serializer.createProperty(property));
    }
    for (ResourceType subType : resourceType.getSubTypeList()) {
      serializeResourceType(subType, vertices);
    }
    for (PropertyRef propertyRef : resourceType.getPropertyRefList()) {
      serializer.createEdge("PropertyRef", serializedResourceType, vertices.get(propertyRef.getRef()));
    }
  }

  private void serializeResource(Resource resource, Map<ASTNode, Object> vertices) {
    Object serializedResource = serializer.createResource(resource);
    vertices.put(resource, serializedResource);
    for (CurrentResourceValue crv : resource.getCurrentResourceValueList()) {
      Object serializedCrv = serializer.createCurrentResourceValue(crv);
      vertices.put(crv, serializedCrv);
      serializer.createEdge("PropertyRef", serializedCrv, vertices.get(crv.getPropertyRef().getRef()));
    }
    serializer.createEdge("Type", serializedResource, vertices.get(resource.getType().getRef()));
    for (Resource subRes : resource.getSubResourceList()) {
      serializeResource(subRes, vertices);
    }
  }

}
