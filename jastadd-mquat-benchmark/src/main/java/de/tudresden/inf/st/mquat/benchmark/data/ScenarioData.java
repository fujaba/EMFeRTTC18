package de.tudresden.inf.st.mquat.benchmark.data;

/**
 * Data describing a single scenario.
 *
 * @author rschoene - Initial contribution
 */
public class ScenarioData {
    private int id;
    public String name = null;
    public int variants;
    public int requests;
    public int depth;
    public double resources;

  public void setId(int id) {
    this.id = id;
    if (this.name == null) {
      this.name = "<Scenario " + Integer.toString(this.id) + ">";
    }
  }

  public int getId() {
    return id;
  }
}
