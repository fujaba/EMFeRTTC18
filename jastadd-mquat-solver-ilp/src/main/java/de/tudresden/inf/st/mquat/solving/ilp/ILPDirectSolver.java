package de.tudresden.inf.st.mquat.solving.ilp;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.solving.SolvingException;
import de.tudresden.inf.st.mquat.utils.LoggingProxyForStdOut;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gnu.glpk.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ILPDirectSolver extends AbstractILPSolver {

  private boolean writeFiles;
  private Path lp, solutionReadable;
  private glp_prob prob;
  private int timeoutInMillis;

  private static boolean listenerAddedToGlpk = false;

  /**
   * Create a new solver with default settings.
   * Default is:
   * <ul>
   *   <li>1 minute timeout</li>
   *   <li>Do not write out ILP and solution files</li>
   * </ul>
   * @see ILPDirectSolver#setWriteFiles(boolean)
   */
  public ILPDirectSolver() {
    super(LogManager.getLogger(ILPDirectSolver.class));
    setWriteFiles(false);
  }

  private GlpkTerminalListener redirectToLogger(final Logger logger, final Level logLevel) {
    return str -> {
      logger.log(logLevel, str.substring(0, str.length() - 1));  // substring to avoid trailing linebreak
      return false;
    };
  }

  public ILPDirectSolver setWriteFiles(boolean writeFiles) {
    this.writeFiles = writeFiles;
    return this;
  }

  @Override
  protected void recomputeTimeoutInSeconds() {
    super.recomputeTimeoutInSeconds();
    // store timeout in milliseconds, if small enough
    long timeoutInMillis = this.timeoutInSeconds * 1000;
    // if smaller than zero, an overflow has occurred
    this.timeoutInMillis = timeoutInMillis > 0 && timeoutInMillis < Integer.MAX_VALUE ? (int) timeoutInMillis : 0;
  }

  @Override
  protected void reset() {
    super.reset();
    this.prob = null;
  }

  protected double solve0(Root model, StopWatch watch, List<IlpVariable> variablesSetToOne) throws SolvingException {
    ILP ilp = model.getILP();

    if (logger.isTraceEnabled()) {
      logger.trace(ilp.printIlp().toString());
    }

    // Create temporary files (if requested)
    if (this.writeFiles) {
      try {
        lp = Files.createTempFile("direct-ilp", null);
        solutionReadable = Files.createTempFile("direct-sol-read", null);
      } catch (IOException e) { throw new SolvingException("Can not create lp or solution file", e); }
    }

    // test if listener is already added to GLPK
    // no atomic get and set needed, as solve() method is synchronized
    if (!listenerAddedToGlpk) {
      GlpkTerminal.addListener(redirectToLogger(logger, Level.DEBUG));
      listenerAddedToGlpk = true;
    }

    // create a glp_prob
    prob = GLPK.glp_create_prob();
    GLPK.glp_set_prob_name(prob, model.description());
    // only add variables not being ignored (which are remaining in the info object)
    GLPK.glp_add_cols(prob, ilp.getInfo().vars.size());

    // helper structure, map IlpVariable to its index
    Map<IlpVariable, Integer> varToIndex = new HashMap<>(ilp.getNumIlpVariable());

    // create bounds
    int colCount = ilp.getNumIlpBound();
    final Set<IlpVariable> toIgnore = new HashSet<>();
    for (int index = 1; index <= ilp.getNumIlpBound(); index++) {
      IlpBound bound = ilp.getIlpBound(index - 1);
      varToIndex.put(bound.getRef(), index);
      switch (bound.getType()) {
        case BINARY:
          GLPK.glp_set_col_kind(prob, index, GLPKConstants.GLP_BV);
          break;
        case ZERO:
          toIgnore.add(bound.getRef());
          --colCount;
          continue;
        default:  // >= 0
          GLPK.glp_set_col_kind(prob, index, GLPKConstants.GLP_IV);
          GLPK.glp_set_col_bnds(prob, index, GLPKConstants.GLP_LO, 0, 0);
          break;
      }
      GLPK.glp_set_col_name(prob, index, bound.getRef().getName());
    }

    // create objective
    GLPK.glp_set_obj_name(prob, model.getObjective().getPropertyRef().getName().getName());
    GLPK.glp_set_obj_dir(prob, ilp.getIlpObjective().getKind() == IlpObjectiveKind.MINIMIZE ?
        GLPKConstants.GLP_MIN : GLPKConstants.GLP_MAX);
    // TODO only variables mentioned in objective are set to a value. Do the others need to be set to zero?
    for (IlpTerm term : ilp.getIlpObjective().getIlpLeftHandSide().getIlpTermList()) {
      if (!toIgnore.contains(term.getRef())) {
        GLPK.glp_set_obj_coef(prob, varToIndex.get(term.getRef()), term.getValue());
      }
    }

    // create a row for each constraint
    int start = GLPK.glp_add_rows(prob, ilp.getNumIlpConstraint());

    for (int rowCounter = start; rowCounter < ilp.getNumIlpConstraint() + start; rowCounter++) {

      IlpConstraint constraint = ilp.getIlpConstraint(rowCounter - start);
      if (logger.isTraceEnabled()) {
        logger.trace("Preparing at {} - {}", rowCounter, constraint.printIlp());
      }
      if (constraint.getIlpLeftHandSide().getNumIlpTerm() == 0) {
        logger.debug("Skipping empty constraint: {}", constraint.printIlp());
        continue;
      }
      // TODO maybe use constraint.getIlpLeftHandSide().getNumIlpTerm() instead of colCount
      SWIGTYPE_p_int ind = GLPK.new_intArray(colCount + 1);
      SWIGTYPE_p_double val = GLPK.new_doubleArray(colCount + 1);
      GLPK.glp_set_row_name(prob, rowCounter, constraint.getName());
      int glpk_kind;
      switch (constraint.getClauseComparator()) {
        case EQ: glpk_kind = GLPKConstants.GLP_FX; break;
        case GE: glpk_kind = GLPKConstants.GLP_LO; break;
        case GT:
          glpk_kind = GLPKConstants.GLP_LO;
          logger.warn("Relaxing constraint to '>= in " + constraint.printIlp().toString());
          break;
        case LE: glpk_kind = GLPKConstants.GLP_UP; break;
        case LT:
          glpk_kind = GLPKConstants.GLP_UP;
          logger.warn("Relaxing constraint to '>= in " + constraint.printIlp().toString());
          break;
        case NE: throw new SolvingException("Can not handle inequality constraint in " + constraint.printIlp().toString());
        default:
          logger.warn("Unknown clause comparator " + constraint.printIlp().toString());
          glpk_kind = 0;
      }
      GLPK.glp_set_row_bnds(prob, rowCounter, glpk_kind, constraint.getRightHandSide(), constraint.getRightHandSide());
      IlpLeftHandSide lhs = constraint.getIlpLeftHandSide();
      int colIndex = 1;
      for (int termIndex = 0; termIndex < lhs.getNumIlpTerm(); termIndex++) {
        IlpTerm term = lhs.getIlpTerm(termIndex);
        if (toIgnore.contains(term.getRef())) {
          continue;
        }
        GLPK.intArray_setitem(ind, colIndex, varToIndex.get(term.getRef()));
        GLPK.doubleArray_setitem(val, colIndex, term.getValue());
        if (logger.isTraceEnabled()) {
          logger.trace("Set ind[{}]={} ({}) and val[{}]={}",
              colIndex, varToIndex.get(term.getRef()), term.getRef().getName(),
              colIndex, term.getValue());
        }
        ++colIndex;
      }
      if (colIndex > 1) {
        GLPK.glp_set_mat_row(prob, rowCounter, colIndex - 1, ind, val);
      } else {
        logger.debug("Skipping constraint with only ignored terms: {}", constraint.printIlp());
      }
      GLPK.delete_intArray(ind);
      GLPK.delete_doubleArray(val);
    }

    // write out the generated problem
    if (this.writeFiles) {
      logger.info("Writing ILP to {}", lp.toAbsolutePath());
      int returnCode = GLPK.glp_write_lp(prob, null, lp.toAbsolutePath().toString());
      if (returnCode != 0) {
        cleanup(watch);
        throw new SolvingException("Could not write to lp file (error code: " + returnCode + ")");
      }
    }

    // now the generation is really finish, note the time and add it to the other generation time
    lastGeneration += watch.time(TimeUnit.MILLISECONDS);
    watch.reset();

    // Setup Parameters. See http://www.maximalsoftware.com/solvopt/optglpk.html
    glp_smcp simplexParam = new glp_smcp();
    GLPK.glp_init_smcp(simplexParam);
    glp_iocp param = new glp_iocp();
    GLPK.glp_init_iocp(param);

    if (logger.isDebugEnabled()) {
      logger.debug("Default simplex parameters: {}", printGetter(simplexParam));
      logger.debug("Default mip parameters: {}", printGetter(param));
    }
    if(timeoutInMillis > 0) {
      logger.debug("Set simplex timeout to {}ms.", timeoutInMillis);
      simplexParam.setTm_lim(timeoutInMillis);
    }

    // TODO maybe presolve is not needed in one of the solvers -- need to be checked
    simplexParam.setPresolve(GLPKConstants.GLP_ON);
//    param.setPresolve(GLPKConstants.GLP_ON);

    GLPK.glp_scale_prob(prob, GLPKConstants.GLP_SF_AUTO);

    // TODO binarize may be needed
//    parm.setBinarize(GLPKConstants.GLP_ON);

    // -- Msg_lev --
    // No output (0)	No output.
    // Error messages (1)	Display error messages only.
    // Normal (2)	Normal output.
    // Complete (3)	Complete output, includes informational messages. (default)
    simplexParam.setMsg_lev(GLPKConstants.GLP_MSG_ALL);
    param.setMsg_lev(GLPKConstants.GLP_MSG_ALL);

    // Solve the generated problem
    int returnCode;
    // First construct basis. TODO maybe not be needed in the end?
//    GLPK.glp_std_basis(prob);
    GLPK.glp_adv_basis(prob, 0);

    // Second, solve the problem, finding an optimal solution
    logger.debug("Start simplex solving");

    returnCode = GLPK.glp_simplex(prob, simplexParam);
    if (returnCode == GLPKConstants.GLP_ETMLIM) {
      logger.info("Simplex Solving was stopped after time limit was reached.");
    } else if (returnCode != 0) {
      cleanup(watch);
      // abuse objective to save return code
      lastObjective = -1000 - returnCode;
      throw new SolvingException("Solving did not finish correctly, reason: " + translateSimplexReturnError(returnCode));
    }

    if (timeoutInMillis > 0) {
      // check how much time is left for MIP after simplex has finished
      int remaining = timeoutInMillis;
      remaining -= watch.time(TimeUnit.MILLISECONDS);
      if (remaining < 0) {
        cleanup(watch);
        this.timedOut = true;
        throw new SolvingException("No time left for MIP solver.");
      }
      logger.debug("Set MIP timeout to {}ms.", remaining);
      param.setTm_lim(remaining);
    }


    // Finally, solve the integer problem
    logger.debug("Start MIP solving");
    returnCode = GLPK.glp_intopt(prob, param);

    if (returnCode == GLPKConstants.GLP_ETMLIM) {
      logger.info("MIP Solving was stopped after time limit was reached.");
      this.timedOut = true;
    } else if (returnCode != 0) {
      cleanup(watch);
      // abuse objective to save return code
      lastObjective = -2000 - returnCode;
      throw new SolvingException("Solving did not finish correctly, reason: " + translateMIPReturnError(returnCode));
    }

    if (this.writeFiles) {
      // write out the found solution
      logger.debug("Solution at {} (readable form)", solutionReadable.toAbsolutePath());
      if (GLPK.glp_print_sol(prob, solutionReadable.toAbsolutePath().toString()) != 0) {
        logger.warn("Could not write solution to " + solutionReadable.toAbsolutePath());
      }
    }

    logMipStatus(prob);

    // Construct the solution
    for (int i = 1; i <= colCount; i++) {
      String name = GLPK.glp_get_col_name(prob, i);
      double val  = GLPK.glp_mip_col_val(prob, i);
      logger.trace("{} (at index {}) = {}", name, i, val);
      if (val == 1) {
        variablesSetToOne.add(ilp.getInfo().vars.get(name));
      }
    }

    return GLPK.glp_mip_obj_val(prob);
  }

  private void logMipStatus(glp_prob prob) {
    int mipStatus = GLPK.glp_mip_status(prob);
    if (mipStatus == GLPKConstants.GLP_UNDEF) {
      logger.error("MIP solution is undefined");
    } else if (mipStatus == GLPKConstants.GLP_OPT) {
      logger.debug("MIP solution is integer optimal");
    } else if (mipStatus == GLPKConstants.GLP_FEAS) {
      logger.warn("MIP solution is integer feasible, however, its optimality (or non-optimality) has " +
          "not been proven, perhaps due to premature termination of the search");
    } else if (mipStatus == GLPKConstants.GLP_NOFEAS) {
      logger.error("problem has no integer feasible solution (proven by the solver)");
    }
  }

  private String translateSimplexReturnError(int returnCode) {
    if (returnCode == GLPKConstants.GLP_EBADB) {
      return "Unable to start the search, because the initial basis specified in the problem object " +
          "is invalid: the number of basic (auxiliary and structural) variables is not the same" +
          "as the number of rows in the problem object.";
    }
    if (returnCode == GLPKConstants.GLP_ESING) {
      return "Unable to start the search, because the basis matrix corresponding to the initial " +
          "basis is singular within the working precision.";
    }
    if (returnCode == GLPKConstants.GLP_ECOND) {
      return "Unable to start the search, because the basis matrix corresponding to the initial " +
          "basis is ill-conditioned, i.e. its condition number is too large.";
    }
    if (returnCode == GLPKConstants.GLP_EBOUND) {
      return "Unable to start the search, because some double-bounded (auxiliary or structural) " +
          "variables have incorrect bounds.";
    }
    if (returnCode == GLPKConstants.GLP_EFAIL) {
      return "The search was prematurely terminated due to the solver failure.";
    }
    if (returnCode == GLPKConstants.GLP_EOBJLL) {
      return "The search was prematurely terminated, because the objective function being maximized " +
      "has reached its lower limit and continues decreasing (the dual simplex only).";
    }
    if (returnCode == GLPKConstants.GLP_EOBJUL) {
      return "The search was prematurely terminated, because the objective function being minimized " +
          "has reached its upper limit and continues increasing (the dual simplex only).";
    }
    if (returnCode == GLPKConstants.GLP_EITLIM) {
      return "The search was prematurely terminated, because the simplex iteration limit has been exceeded.";
    }
    if (returnCode == GLPKConstants.GLP_ENOPFS) {
      return "The LP problem instance has no primal feasible solution.";
    }
    if (returnCode == GLPKConstants.GLP_ENODFS) {
      return "The LP problem instance has no dual feasible solution.";
    }
    return "Unknown error code for simplex: " + returnCode;
  }

  private String translateMIPReturnError(int returnCode) {
    if (returnCode == GLPKConstants.GLP_EBOUND) {
      return "Unable to start the search, because some double-bounded variables have incorrect " +
          "bounds or some integer variables have non-integer (fractional) bounds.";
    }
    if (returnCode == GLPKConstants.GLP_EROOT) {
      return "Unable to start the search, because optimal basis for initial LP relaxation is not provided.";
    }
    if (returnCode == GLPKConstants.GLP_ENOPFS) {
      return "Unable to start the search, because LP relaxation of the MIP problem instance has " +
          "no primal feasible solution.";
    }
    if (returnCode == GLPKConstants.GLP_ENODFS) {
      return "Unable to start the search, because LP relaxation of the MIP problem instance has " +
          "no dual feasible solution. In other word, this code means that if the LP relaxation " +
          "has at least one primal feasible solution, its optimal solution is unbounded, so if the " +
          "MIP problem has at least one integer feasible solution, its (integer) optimal solution " +
          "is also unbounded.";
    }
    if (returnCode == GLPKConstants.GLP_EFAIL) {
      return "The search was prematurely terminated due to the solver failure.";
    }
    if (returnCode == GLPKConstants.GLP_EMIPGAP) {
      return "The search was prematurely terminated, because the relative mip gap tolerance has " +
          "been reached.";
    }
    if (returnCode == GLPKConstants.GLP_ESTOP) {
      return "The search was prematurely terminated by application.";
    }
    return "Unknown error code for MIP: " + returnCode;
  }

  private String printGetter(Object parm) {
    StringBuilder sb = new StringBuilder();
    for (Method method : parm.getClass().getMethods()) {
      if (method.getName().startsWith("get")) {
        sb.append(method.getName()).append('=');
        try {
          Object result = method.invoke(parm);
          sb.append(result).append(',');
        } catch (IllegalAccessException | InvocationTargetException e) {
          // silently ignore exception
        }
      }
    }
    sb.setCharAt(sb.length() - 1, '.');
    return sb.toString();
  }

  @Override
  protected void cleanup(StopWatch watch) {
    super.cleanup(watch);
    GLPK.glp_delete_prob(prob);
    prob = null;
  }

  @Override
  public String getName() {
    return "ilp-direct";
  }
}
