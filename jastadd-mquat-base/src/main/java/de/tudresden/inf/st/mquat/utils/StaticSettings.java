package de.tudresden.inf.st.mquat.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings specific to solvers using attributes.
 *
 * @author rschoene - Initial contribution
 */
public class StaticSettings {

  private static StaticSettings theInstance;
  private Map<String, Object> values;

  private StaticSettings() {
    values = new HashMap<>();
  }

  public static StaticSettings getInstance() {
    if (theInstance == null) {
      theInstance = new StaticSettings();
    }
    return theInstance;
  }

  public static void put(String key, Object value) {
    getInstance().values.put(key, value);
  }

  public static Object get(String key) {
    return getInstance().values.get(key);
  }

}
