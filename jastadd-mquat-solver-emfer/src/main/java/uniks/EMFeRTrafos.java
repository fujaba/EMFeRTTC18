package uniks;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import de.tudresden.inf.st.mquat.solving.SolverUtils;
import uniks.ttc18.EAssignment;
import uniks.ttc18.ESolution;
import uniks.ttc18.Ttc18Factory;

import java.util.ArrayList;

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
         String nodeName = model.getHardwareModel().getResource(resourceNum).getName().toString();
         resourceNum++;
         topAssignment.setNodeName(nodeName);
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
}
