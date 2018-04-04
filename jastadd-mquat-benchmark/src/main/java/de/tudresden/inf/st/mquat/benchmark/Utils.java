package de.tudresden.inf.st.mquat.benchmark;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Utils {

  static ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    return mapper;
  }

  private static File readFromResource(String filename) throws IOException {
    URL basicSettingsURL = CustomBenchmarkMain.class.getClassLoader().getResource(filename);
    if (basicSettingsURL == null) {
      System.err.println();
      throw new IOException("Could not access " + filename + ". Exiting.");
    }
    return new File(basicSettingsURL.getFile());
  }

  static <T> T readFromResource(ObjectMapper mapper, String filename, Class<T> clazz) throws IOException {
    File basicSettingsFile = readFromResource(filename);
    T result = null;
    try {
      result = mapper.readValue(basicSettingsFile, clazz);
    } catch (Exception e) {
      System.err.println("Could not load '" + filename + "'. Exiting.");
      e.printStackTrace();
      System.exit(2);
    }
    return result;
  }

  public static <T> T nonNullOrDefault(T newValue, T defaultValue) {
    return newValue != null ? newValue : defaultValue;
  }

}
