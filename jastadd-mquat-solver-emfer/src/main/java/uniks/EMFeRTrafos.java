package uniks;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.jastadd.model.List;
import de.tudresden.inf.st.mquat.solving.SolverUtils;
import org.eclipse.emf.ecore.EObject;
import uniks.ttc18.EAssignment;
import uniks.ttc18.EChoice;
import uniks.ttc18.ESolution;
import uniks.ttc18.Ttc18Factory;

import java.util.*;

public class EMFeRTrafos
{
   private Root model;

   public EMFeRTrafos(Root model)
   {
      this.model = model;
   }

   public int resourceNum = 0;

   public void createTopLevelAssignments(ESolution eSolution)
   {
      org.eclipse.emf.common.util.EList<EAssignment> assignments = eSolution.getAssignments();

      if ( ! assignments.isEmpty())
      {
         return;
      }

      for ( Request request : model.getRequests())
      {
         String requName = request.getName().toString();
         ComponentRef target = request.getTarget();
         Name targetName = target.getName();

         EAssignment topAssignment = Ttc18Factory.eINSTANCE.createEAssignment();
         topAssignment.setRequestName(requName);
         topAssignment.setCompName(targetName.toString());
         // String nodeName = model.getHardwareModel().getResource(resourceNum).getName().toString();
         // resourceNum++;
         // topAssignment.setNodeName(nodeName);
         assignments.add(topAssignment);
      }
   }

   public void createSubAssignments(ESolution eSolution, EAssignment newAssignment)
   {
      String compName = newAssignment.getCompName();
      Component comp = findComp(model, compName);
      Implementation implementation = comp.getImplementation(0);
      String implName = implementation.getName().toString();
      newAssignment.setImplName(implName);


      for (ComponentRequirement componentRequirement : implementation.getComponentRequirements())
      {
         String requCompName = componentRequirement.getComponentRef().getRef().getName().toString();

         EAssignment kidAssignment = Ttc18Factory.eINSTANCE.createEAssignment();
         kidAssignment.setRequestName(implName);
         kidAssignment.setCompName(requCompName);
         String nodeName = model.getHardwareModel().getResource(resourceNum).getName().toString();
         resourceNum++;
         kidAssignment.setNodeName(nodeName);

         newAssignment.getAssignments().add(kidAssignment);

         createSubAssignments(eSolution, kidAssignment);
      }

   }


   public Solution transformPartial(ESolution eSolution)
   {
      Solution result = new Solution();
      result.setModel(model);

      // top level
      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         Assignment dAssignment = new Assignment();
         Request request = findRequest(eAssignment.getRequestName());
         dAssignment.setRequest(request);
         dAssignment.setTopLevel(true);

         Component comp = findComp(model, eAssignment.getCompName());
         Implementation implementation = findImplementation(comp, eAssignment.getImplName());
         dAssignment.setImplementation(implementation);

         if (eAssignment.getNodeName() == null)
         {
            continue;
         }

         transformPartialSubAssignments(dAssignment, eAssignment, implementation);

         for (Instance instance : implementation.getResourceRequirement().getInstances())
         {
            Resource resource =  findResource(eAssignment.getNodeName());
            ResourceMapping resourceMapping = new ResourceMapping(instance, resource, new List<>());

            SolverUtils.populateResourceMapping(resourceMapping, implementation.getResourceRequirement(), resource);

            dAssignment.setResourceMapping(resourceMapping);

//            if (dAssignment.isValid())
//            {
//               System.out.println("assignment is valid " + dAssignment.getImplementation().getName() + " on " + resource.getName());
//            }
         }

         result.addAssignment(dAssignment);
      }

      return result;
   }


   private void transformPartialSubAssignments(Assignment dAssignment, EAssignment eAssignment, Implementation implementation)
   {
      for (EAssignment eSubAssignment : eAssignment.getAssignments())
      {
         ComponentMapping componentMapping = new ComponentMapping();

         Assignment dSubAssignment = new Assignment();
         Request request = dAssignment.getRequest();
         dSubAssignment.setRequest(request);
         dSubAssignment.setTopLevel(false);

         Component subComp = findComp(model, eSubAssignment.getCompName());
         Implementation subImpl = findImplementation(subComp, eSubAssignment.getImplName());

         dSubAssignment.setImplementation(subImpl);

         Instance theInstance = findInstance(implementation, "the_" + eSubAssignment.getCompName());

         componentMapping.setInstance(theInstance);
         componentMapping.setAssignment(dSubAssignment);
         dAssignment.getComponentMappings().add(componentMapping);

         transformPartialSubAssignments(dSubAssignment, eSubAssignment, subImpl);

         if (eSubAssignment.getNodeName() == null)
         {
            continue;
         }

         for (Instance instance : subImpl.getResourceRequirement().getInstances())
         {
            Resource resource = findResource(eSubAssignment.getNodeName());
            ResourceMapping resourceMapping = new ResourceMapping(instance, resource, new List<>());
            SolverUtils.populateResourceMapping(resourceMapping, subImpl.getResourceRequirement(), resource);
            dSubAssignment.setResourceMapping(resourceMapping);

            if (dSubAssignment.isValid())
            {
               break;
            }
         }

      }
   }



   ArrayList<Resource> availableResources = null;

   public Solution transform(ESolution eSolution)
   {
      Solution result = new Solution();
      result.setModel(model);

      availableResources = new ArrayList();
      List<Resource> resourceList = model.getHardwareModel().getResources();
      for (Resource r : resourceList)
      {
         availableResources.add(r);
      }



      // top level
      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         Assignment dAssignment = new Assignment();
         Request request = findRequest(eAssignment.getRequestName());
         dAssignment.setRequest(request);
         dAssignment.setTopLevel(true);

         Component comp = findComp(model, eAssignment.getCompName());
         Implementation implementation = findImplementation(comp, eAssignment.getImplName());
         if (implementation == null)
         {
            continue;
         }
         dAssignment.setImplementation(implementation);

         transformSubAssignments(dAssignment, eAssignment, implementation);

         for (Instance instance : implementation.getResourceRequirement().getInstances())
         {
            for (Resource resource : availableResources)
            {
               ResourceMapping resourceMapping = new ResourceMapping(instance, resource, new List<>());

               SolverUtils.populateResourceMapping(resourceMapping, implementation.getResourceRequirement(), resource);

               dAssignment.setResourceMapping(resourceMapping);

               if (dAssignment.isValid())
               {
                  availableResources.remove(resource);
                  break;
               }
            }
         }

         result.addAssignment(dAssignment);
      }

      return result;
   }


   private void transformSubAssignments(Assignment dAssignment, EAssignment eAssignment, Implementation implementation)
   {
      for (EAssignment eSubAssignment : eAssignment.getAssignments())
      {
         ComponentMapping componentMapping = new ComponentMapping();

         Assignment dSubAssignment = new Assignment();
         Request request = dAssignment.getRequest();
         dSubAssignment.setRequest(request);
         dSubAssignment.setTopLevel(false);

         Component subComp = findComp(model, eSubAssignment.getCompName());
         Implementation subImpl = findImplementation(subComp, eSubAssignment.getImplName());
         if (subImpl == null)
         {
            continue;
         }
         dSubAssignment.setImplementation(subImpl);

         for (Instance instance : subImpl.getResourceRequirement().getInstances())
         {
            for (Resource resource : availableResources)
            {
               ResourceMapping resourceMapping = new ResourceMapping(instance, resource, new List<>());
               SolverUtils.populateResourceMapping(resourceMapping, subImpl.getResourceRequirement(), resource);
               dSubAssignment.setResourceMapping(resourceMapping);

               if (dSubAssignment.isValid())
               {
                  availableResources.remove(resource);
                  break;
               }
            }
         }

         Instance instance = findInstance(implementation, "the_" + eSubAssignment.getCompName());

         componentMapping.setInstance(instance);
         componentMapping.setAssignment(dSubAssignment);
         dAssignment.getComponentMappings().add(componentMapping);

         transformSubAssignments(dSubAssignment, eSubAssignment, subImpl);
      }
   }


   private Component findComp(Root model, String compName)
   {
      for (Component comp : model.getSoftwareModel().getComponents())
      {
         if (comp.getName().toString().equals(compName))
         {
            return comp;
         }
      }
      return null;
   }


   private Instance findInstance(Implementation implementation, String compName)
   {
      for (ComponentRequirement componentRequirement : implementation.getComponentRequirements())
      {
         for (Instance instance : componentRequirement.getInstances())
         {
            if (instance.getName().toString().equals(compName))
            {
               return instance;
            }
         }
      }
      return null;
   }


   private Resource findResource(String nodeName)
   {
      for (Resource r : model.getHardwareModel().getResources())
      {
         if (r.getName().toString().equals(nodeName))
         {
            return r;
         }
      }
      return null;
   }


   private Implementation findImplementation(Component comp, String implName)
   {
      for (Implementation impl : comp.getImplementations())
      {
         if (impl.getName().toString().equals(implName))
         {
            return impl;
         }
      }
      return null;
   }

   private Request findRequest(String requName)
   {
      for (Request r : model.getRequests())
      {
         if (r.getName().toString().equals(requName))
         {
            return r;
         }
      }
      return null;
   }

   public Collection<EAssignment> getOpenAssignments(ESolution eSolution)
   {
      ArrayList<EAssignment> result = new ArrayList<>();

      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         if (eAssignment.getImplName() == null)
         {
            result.add(eAssignment);
         }

         getOpenSubAssignments(eAssignment, result);
      }

      return result;
   }

   private void getOpenSubAssignments(EAssignment eAssignment, ArrayList<EAssignment> result)
   {
      for (EAssignment subAssignment : eAssignment.getAssignments())
      {
         if (subAssignment.getImplName() == null)
         {
            result.add(subAssignment);
         }

         getOpenSubAssignments(subAssignment, result);
      }
   }

   public Collection<EObject> getImplementationChoices(EObject root)
   {
      ArrayList<EObject> result = new ArrayList<>();

      ESolution eSolution = (ESolution) root;

      for (EAssignment eAssignment : getOpenAssignments(eSolution))
      {
         Component comp = findComp(model, eAssignment.getCompName());

         for (Implementation dImpl : comp.getImplementations())
         {
            String implName = dImpl.getName().toString();
            eAssignment.setImplName(implName);
//            Collection computeNodeChoices = getComputeNodeChoices(eSolution, eAssignment);
//            if (computeNodeChoices.size() == 0)
//            {
//               continue;
//            }

            EChoice eImpl = Ttc18Factory.eINSTANCE.createEChoice();
            eImpl.setAssignment(eAssignment);
            eImpl.setResName(dImpl.getName().toString());
            result.add(eImpl);
         }

         eAssignment.setImplName(null);

         return result;
      }

      return result;
   }

   public void applyImplementationChoice(EObject root, EObject handle)
   {
      EChoice eImpl = (EChoice) handle;

      EAssignment topAssignment = eImpl.getAssignment();
      topAssignment.setImplName(eImpl.getResName());

      Component comp = findComp(model, topAssignment.getCompName());

      Implementation dImpl = findImplementation(comp, eImpl.getResName());

      for (ComponentRequirement componentRequirement : dImpl.getComponentRequirementList())
      {
         String kidCompName = componentRequirement.getComponentRef().getRef().getName().toString();

         EAssignment kidAssignment = Ttc18Factory.eINSTANCE.createEAssignment();
         kidAssignment.setRequestName(topAssignment.getRequestName());
         kidAssignment.setCompName(kidCompName);

         topAssignment.getAssignments().add(kidAssignment);
      }
   }

   public Collection getComputeNodeChoices(EObject root)
   {
      ESolution eSolution = (ESolution) root;
      ArrayList<EChoice> result = new ArrayList<>();

      // find assignment that is ready to be deployed
      EAssignment eAssignment = getNextUndeployedAssignment(eSolution);

      if (eAssignment == null)
      {
         return result;
      }

      getComputeNodeChoices(eSolution, result, eAssignment);

      return result;
   }


   private Collection getComputeNodeChoices(ESolution eSolution, EAssignment eAssignment)
   {
      ArrayList<EChoice> result = new ArrayList<>();

      getComputeNodeChoices(eSolution, result, eAssignment);

      return result;
   }


   private void getComputeNodeChoices(ESolution eSolution, ArrayList<EChoice> result, EAssignment eAssignment)
   {
      HashSet<String> alreadInUseComputeNodes = getAlreadInUseComputeNodes(eSolution);

      // loop through all compute nodes
      for (Resource compNode : model.getHardwareModel().getResourceList())
      {
         String compNodeName = compNode.getName().toString();

//         if (alreadInUseComputeNodes.contains(compNodeName))
//         {
//            continue;
//         }

         eAssignment.setNodeName(compNodeName);

         // create dSolution
         Solution dSolution = transformPartial(eSolution);


         // if assignment is valid, add choice
         boolean allValid = checkAssignments(dSolution, eAssignment.getImplName());

         if (allValid)
         {
            EChoice eChoice = Ttc18Factory.eINSTANCE.createEChoice();
            eChoice.setAssignment(eAssignment);
            eChoice.setResName(compNodeName);
            result.add(eChoice);

//            if (result.size() >= 4)
//            {
//               break;
//            }
         }
      }

      eAssignment.setNodeName(null);

//      if (result.size() == 0)
//      {
//         System.out.println("did not find hardware for \n" + eAssignment);
//      }
   }

   private boolean checkAssignments(Solution dSolution, String implName)
   {
      for (Assignment dAssignment : dSolution.getAssignmentList())
      {
         if (dAssignment.getImplementation().getName().toString().equals(implName))
         {
            try
            {
               return dAssignment.isValid();
            }
            catch (Exception e)
            {
               return false;
            }
         }
               // check kids
         if (checkAssignments(dAssignment, implName))
         {
            return true;
         }
      }

      return false;
   }


   private boolean checkAssignments(Assignment parent, String implName)
   {
      for (ComponentMapping componentMapping : parent.getComponentMappingList())
      {
         Assignment dAssignment = componentMapping.getAssignment();

         if (dAssignment.getImplementation().getName().toString().equals(implName))
         {
            return dAssignment.isValid();
         }

         if (checkAssignments(dAssignment, implName))
         {
            return true;
         }
      }

      return false;
   }


   private EAssignment getNextUndeployedAssignment(ESolution eSolution)
   {
      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         if ( ! allAssignmentsInTreeHaveAnImplementation(eAssignment))
         {
            continue;
         }

         if (eAssignment.getNodeName() == null)
         {
            return eAssignment;
         }

         EAssignment kid = getNextUndeployedAssignment(eAssignment);

         if (kid != null)
         {
            return kid;
         }
      }

      return null;
   }

   private boolean allAssignmentsInTreeHaveAnImplementation(EAssignment eAssignment)
   {
      if (eAssignment.getImplName() == null)
      {
         return false;
      }

      for (EAssignment kid : eAssignment.getAssignments())
      {
         if (! allAssignmentsInTreeHaveAnImplementation(kid))
         {
            return false;
         }
      }

      return true;
   }

   private EAssignment getNextUndeployedAssignment(EAssignment eAssignment)
   {
      for (EAssignment kid : eAssignment.getAssignments())
      {
         if (kid.getImplName() == null)
         {
            continue;
         }

         if (kid.getNodeName() == null)
         {
            return kid;
         }

         EAssignment grandKid = getNextUndeployedAssignment(kid);

         if (grandKid != null)
         {
            return grandKid;
         }
      }

      return null;
   }



   private HashSet<String> getAlreadInUseComputeNodes(ESolution eSolution)
   {
      HashSet<String> result = new HashSet<>();

      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         getAlreadInUseComputeNodes(eAssignment, result);
      }

      return result;
   }



      private void getAlreadInUseComputeNodes(EAssignment eAssignment, Set<String> alreadInUseComputeNodes)
   {
      if (eAssignment.getNodeName() != null)
      {
         alreadInUseComputeNodes.add(eAssignment.getNodeName());
      }

      for (EAssignment kidAssignment : eAssignment.getAssignments())
      {
         getAlreadInUseComputeNodes(kidAssignment, alreadInUseComputeNodes);
      }
   }

   private Collection<EAssignment> getNoComputeNodeAssignments(ESolution eSolution)
   {
      ArrayList<EAssignment> result = new ArrayList<>();

      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         if (eAssignment.getNodeName() == null)
         {
            result.add(eAssignment);
         }

         getOpenSubAssignments(eAssignment, result);
      }

      return result;
   }



   public void assignComputeNode(EObject root, EObject node)
   {
      EChoice eChoice = (EChoice) node;
      eChoice.getAssignment().setNodeName(eChoice.getResName());
   }


   public double getNumberOfSolvedIssues(EObject root)
   {
      ESolution eSolution = (ESolution) root;

      int result = 0;

      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         result +=  getNumberOfAssignmentSolvedIssues(eAssignment);
      }

      return result;
   }

   private int getNumberOfAssignmentSolvedIssues(EAssignment eAssignment)
   {
      int result = 0;
      if (eAssignment.getImplName() != null) result++;
      if (eAssignment.getNodeName() != null) result++;

      for (EAssignment kid : eAssignment.getAssignments())
      {
         result += getNumberOfAssignmentSolvedIssues(kid);
      }

      return result;
   }


   public double getNumberOfOpenIssues(EObject root)
   {
      ESolution eSolution = (ESolution) root;

      int result = 0;

      for (EAssignment eAssignment : eSolution.getAssignments())
      {
         result +=  getNumberOfAssignmentOpenIssues(eAssignment);
      }

      return result;
   }

   private int getNumberOfAssignmentOpenIssues(EAssignment eAssignment)
   {
      int result = 0;
      if (eAssignment.getImplName() == null) result++;
      if (eAssignment.getNodeName() == null) result++;

      for (EAssignment kid : eAssignment.getAssignments())
      {
         result += getNumberOfAssignmentOpenIssues(kid);
      }

      return result;
   }
}
