package de.tudresden.inf.st.mquat.utils;

public class TestUtils {

  public static boolean shouldTestLongRunning() {
    return Boolean.valueOf(System.getProperty(
        "de.tudresden.inf.st.mquat.longRunningTest", Boolean.FALSE.toString()));
  }
}
