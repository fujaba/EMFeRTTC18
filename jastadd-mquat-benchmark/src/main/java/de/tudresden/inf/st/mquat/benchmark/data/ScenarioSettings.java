package de.tudresden.inf.st.mquat.benchmark.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.logging.log4j.Level;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ScenarioSettings {

  public String path;
  public String logLevel = Level.WARN.name();
  public List<String> solvers;
  public int timeoutValue;
  public String timeoutUnit;
  public int seed;
  public int repetitions = 1;
  public List<ScenarioData> scenarios;
}
