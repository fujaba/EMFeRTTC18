package de.tudresden.inf.st.mquat.solving;

import org.gnu.glpk.GLPK;
import org.junit.Assert;
import org.junit.Test;

public class GLPKTest {

  @Test
  public void glpkJavaInstalled() {
    Assert.assertNotNull(GLPK.glp_version());
  }
}
