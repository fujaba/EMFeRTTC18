package de.tudresden.inf.st.mquat.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;

public class LoggingProxyForStdOut extends PrintStream {

  private final PrintStream formerOut;
  private final Logger logger;
  private final Level logLevel;

  public LoggingProxyForStdOut(Logger logger, Level logLevel) {
    super(System.out);
    this.formerOut = System.out;
    this.logger = logger;
    this.logLevel = logLevel;
    System.setOut(this);
  }

  @Override
  public void print(final String x) {
    logger.log(logLevel, x);
  }

  @Override
  public void println(String x) {
    logger.log(logLevel, x);
  }

  @Override
  public void close() {
    System.setOut(formerOut);
  }

}
