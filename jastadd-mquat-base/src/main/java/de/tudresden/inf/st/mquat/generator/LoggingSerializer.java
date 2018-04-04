package de.tudresden.inf.st.mquat.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Simple serializer logging out new nodes to a logger.
 *
 * @author rschoene - Initial contribution
 */
public class LoggingSerializer extends ModelSerializer {

  private Logger logger = LogManager.getLogger(LoggingSerializer.class);

  @Override
  public void initModel() {
    logger.info("InitModel");
  }

  @Override
  public void persistModel() {
    logger.info("PersistModel");
  }

  @Override
  protected Object createVertex(long id, String type, Map<String, ?> attributes) {
    logger.info("CreateVertex (id={}, type={} attributes={})", id, type, attributes);
    return id;
  }

  @Override
  protected void createEdge(String label, Object from, Object to) {
    logger.info("CreateEdge (label={}) from {} to {}", label, from, to);
  }
}
