package de.tudresden.inf.st.mquat.generator;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.utils.MapCreator;

import java.util.Map;

import static de.tudresden.inf.st.mquat.utils.MapCreator.e;

/**
 * Customized creation of models.
 *
 * @author rschoene - Initial contribution
 */
public abstract class ModelSerializer {

  private long id;

  public ModelSerializer() {
    id = 0;
  }

  protected long getId() {
    return id;
  }

  public abstract void initModel();

  public abstract void persistModel();

  public Object createProperty(Property property) {
    return createVertex("Property", MapCreator.of(
        e("Name", property.getName()),
        e("Unit", property.getUnit())));
  }

  public Object createResourceType(ResourceType resourceType) {
    return createVertex("ResourceType", MapCreator.of(
        e("Name", resourceType.getName()),
        e("Container", resourceType.getContainer())));
  }

  public Object createResource(Resource resource) {
    return createVertex("Resource", MapCreator.of(
        e("Name", resource.getName())));
  }

  public Object createCurrentResourceValue(CurrentResourceValue crv) {
    return createVertex("CurrentResourceValue", MapCreator.of(
        e("Value", crv.getValue().evalAsDouble())));
  }

  public Object createMetaParameter(MetaParameter metaParameter) {
    return createVertex("MetaParameter", MapCreator.of(
        e("Name", metaParameter.getName())));
  }

  public Object createComponent(Component component) {
    return createVertex("Component", MapCreator.of(
        e("Name", component.getName())));
  }

  public Object createImplementation(Implementation implementation) {
    return createVertex("Implementation", MapCreator.of(
        e("Name", implementation.getName())));
  }

  public Object createComponentRequirement() {
    return createVertex("ComponentRequirement", MapCreator.of());
  }

  public Object createResourceRequirement() {
    return createVertex("ResourceRequirement", MapCreator.of());
  }

  public Object createInstance(Instance instance) {
    return createVertex("Instance", MapCreator.of(
        e("Name", instance.getName())));
  }

  public Object createClause(Clause Clause) {
    return createVertex("Clause", MapCreator.of(
        e("ClauseType", Clause.getClauseType()),
        e("ClauseComparator", Clause.getClauseComparator())));
  }

  public Object createDesignator(Designator designator) {
    Object result = createVertex(designator.getClass().getSimpleName(), MapCreator.of());
    if (designator instanceof QualifiedNameDesignator) {
      throw new RuntimeException("Should not exist anymore: " + designator);
    }
    return result;
  }

  public Object createExpression(Expression expression) {
    return createVertex(expression.getClass().getSimpleName(), MapCreator.of());
  }

  public Object createLiteralExpression(LiteralExpression expression) {
    return createVertex("RealLiteralExpression", MapCreator.of(
        e("Value", expression.getValue())));
  }

  public Object createRequest(Request request) {
    return createVertex("Request", MapCreator.of(
        e("Name", request.getName())));
  }

  public Object createMetaParameterAssignment() {
    return createVertex("MetaParameterAssignment", MapCreator.of());
  }

  public Object createObjective(Objective objective) {
    return createVertex("Objective", MapCreator.of(
        e("Agg", objective.getAgg())));
  }

  // TODO add and use parent parameter
  public final Object createVertex(final String type, final Map<String, ?> attributes) {
    return createVertex(id++, type, attributes);
  }

  protected abstract Object createVertex(long id, String type, Map<String, ?> attributes);

  protected abstract void createEdge(String label, Object from, Object to);

}
