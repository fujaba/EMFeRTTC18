package uniks;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.solving.BenchmarkableSolver;
import de.tudresden.inf.st.mquat.solving.Solver;
import de.tudresden.inf.st.mquat.solving.SolverUtils;
import de.tudresden.inf.st.mquat.solving.SolvingException;
import de.tudresden.inf.st.mquat.utils.StopWatch;
import emfer.EMFeR;
import emfer.reachability.ReachableState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import uniks.ttc18.EAssignment;
import uniks.ttc18.ESolution;
import uniks.ttc18.Ttc18Factory;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class EMFeRSolver implements BenchmarkableSolver {

   private static final Logger logger = LogManager.getLogger(EMFeRSolver.class);

   private Solution lastSolution;
   private long lastSolvingTime;

   private int solutionCounter;

   private StopWatch stopWatch;

   private long maxSolvingTime;
   private boolean timedOut;

   public EMFeRSolver() {
      this(Long.MAX_VALUE);
   }

   public EMFeRSolver(long maxSolvingTime) {
      this.maxSolvingTime = maxSolvingTime;
      reset();
   }

   private static void assignResource(Assignment assignment, Resource resource) {
      Implementation impl = assignment.getImplementation();

      ResourceMapping mapping = new ResourceMapping(impl.getResourceRequirement().getInstance(0), resource, new de.tudresden.inf.st.mquat.jastadd.model.List<>());
      SolverUtils.populateResourceMapping(mapping, impl.getResourceRequirement(), resource);
      assignment.setResourceMapping(mapping);
   }

   private int checkAssignment(Solution solution, List<Solution> solutions, List<Assignment> assignments, List<Set<Resource>> possibleResources, int index, Stack<Resource> usedResources) {
      int checkCounter = 0;

      Assignment assignment = assignments.get(index);
      for (Resource resource : possibleResources.get(index)) {

         if (stopWatch.time(TimeUnit.MILLISECONDS) > maxSolvingTime) {
            return checkCounter;
         }

         if (usedResources.contains(resource)) continue;
         assignResource(assignment, resource);
         usedResources.push(resource);
         checkCounter++;
         if (index == assignments.size() - 1) {
            if (solution.isValid()) {
               solutionCounter++;
               if (solutions.isEmpty() || solution.computeObjective() < solutions.get(solutions.size() - 1).computeObjective()) {
                  Solution clone = solution.deepCopy();
                  solutions.add(clone);
                  logger.info("found a better solution with an objective of {}.", solution.computeObjective());
               }

            }
         } else {
            checkCounter += checkAssignment(solution, solutions, assignments, possibleResources, index + 1, usedResources);
         }
         usedResources.pop();
      }
      return checkCounter;
   }

   @Override
   public Solution solve(Root model) throws SolvingException
   {
      stopWatch = StopWatch.start();

      EMFeRTrafos emFeRTrafos = new EMFeRTrafos(model);

      ESolution eSolution = Ttc18Factory.eINSTANCE.createESolution();
      emFeRTrafos.createTopLevelAssignments(eSolution);

      EDataType eString = EcorePackage.eINSTANCE.getEString();
      EObject eObj = null;

      // HashMap<String, Collection<String>> implementation2NodeListMap = emFeRTrafos.computeImplementation2NodeListMap();

      EMFeR emfer = new EMFeR()
              .withStart(eSolution)
              .withMaxNoOfNewStates(100 * 1000)
              .withMetric(root -> -1 * emFeRTrafos.getNumberOfOpenIssues(root))
              .withTrafo("choose implementation", root -> emFeRTrafos.getImplementationChoices(root), (root, impl) -> emFeRTrafos.applyImplementationChoice(root, impl))
              .withTrafo("assign node", root -> emFeRTrafos.getComputeNodeChoices(root), (root, node) -> emFeRTrafos.assignComputeNode(root, node))
              ;

      int noOfStates = emfer.explore();

      Solution bestSolution = null;
      double bestObjective = Double.MAX_VALUE;
      int noOfCompleteSolutions = 0;

      System.out.println("emfer searching for best solution in " + noOfStates);

      for (ReachableState state : emfer.getReachabilityGraph().getStates())
      {
         ESolution newSolution = (ESolution) state.getRoot();

         if (bestSolution == null)
         {
            bestSolution = emFeRTrafos.transformPartial(newSolution);
         }

         if (emFeRTrafos.getNumberOfOpenIssues(newSolution) == 0)
         {
            noOfCompleteSolutions ++;
            Solution emferSolution = emFeRTrafos.transformPartial(newSolution);

            if ( ! emferSolution.isValid())
            {
               // System.out.println("solution is invalid: " + newSolution);
               continue;
            }

            double newObjective = emferSolution.computeObjective();

            // System.out.println("objective " + newObjective + "\n" + newSolution);
            if (newObjective < bestObjective)
            {
               bestSolution = emferSolution;
               bestObjective = newObjective;
               System.out.println("Found solution with objective: " + bestObjective);
            }
         }
      }

      if (bestSolution != null)
      {
         logger.info("emfer found a solution with an objective of {}. Number of complete solutions {} ", bestSolution.computeObjective(), noOfCompleteSolutions );
      }

      lastSolvingTime = stopWatch.time(TimeUnit.MILLISECONDS);

      return bestSolution;
   }

   private boolean isCompleteSolution(ESolution eSolution)
   {
      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         if ( ! isCompleteSolution(eAssignment))
         {
            return false;
         }
      }

      return true;
   }

   private boolean isCompleteSolution(EAssignment eAssignment)
   {
      if (eAssignment.getImplName() != null && eAssignment.getNodeName() != null)
      {
         for (EAssignment kid : eAssignment.getAssignments())
         {
            if ( ! isCompleteSolution(kid))
            {
               return false;
            }
         }

         return true;
      }
      else
      {
         return false;
      }
   }


   private void reset() {
      this.lastSolution = null;
      this.solutionCounter = 0;
      this.lastSolvingTime = 0;
      this.timedOut = false;
   }

   @Override
   public String getName() {
      return "emfer";
   }

   @Override
   public long getLastSolvingTime() {
      return lastSolvingTime;
   }

   @Override
   public double getLastObjective() {
      if (lastSolution != null) {
         return lastSolution.computeObjective();
      } else {
         // TODO throw exception or do something reasonable
         return 0d;
      }
   }

   @Override
   public Solver setTimeout(long timeoutValue, TimeUnit timeoutUnit) {
      this.maxSolvingTime = timeoutUnit.toMillis(timeoutValue);
      return this;
   }

   @Override
   public boolean hadTimeout() {
      return this.timedOut;
   }
}
