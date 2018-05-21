package uniks;

import de.tudresden.inf.st.mquat.jastadd.model.*;
import uniks.ttc18.EAssignment;
import uniks.ttc18.ECompMapping;
import uniks.ttc18.ESolution;
import uniks.ttc18.Ttc18Factory;

import java.io.IOException;
import java.nio.file.*;

public class EMFeRTrafos
{
   private Root model;

   public EMFeRTrafos(Root model)
   {
      this.model = model;
   }

   public void createTopLevelMappings(ESolution eSolution)
   {
      org.eclipse.emf.common.util.EList<ECompMapping> compMappings = eSolution.getCompMappings();

      if ( ! compMappings.isEmpty())
      {
         return;
      }

      for ( Request request : model.getRequests())
      {
         String requName = request.getName().toString();
         ComponentRef target = request.getTarget();
         Name targetName = target.getName();

         ECompMapping eCompMapping = Ttc18Factory.eINSTANCE.createECompMapping();
         eCompMapping.setRequName(requName);
         eCompMapping.setCompName(targetName.toString());
         compMappings.add(eCompMapping);
      }
   }

   public void createAssignments(ESolution eSolution, ECompMapping compMapping)
   {
      String compName = compMapping.getCompName();
      Component comp = findComp(model, compName);
      Implementation implementation = comp.getImplementation(0);
      EAssignment eAssignment = Ttc18Factory.eINSTANCE.createEAssignment();
      String implName = implementation.getName().toString();
      eAssignment.setImplName(implName);
      compMapping.setAssignment(eAssignment);


      for (ComponentRequirement componentRequirement : implementation.getComponentRequirements())
      {
         String requCompName = componentRequirement.getComponentRef().getRef().getName().toString();

         ECompMapping subMapping = Ttc18Factory.eINSTANCE.createECompMapping();
         subMapping.setRequName(implName);
         subMapping.setCompName(requCompName);
         eAssignment.getCompMappings().add(subMapping);

         createAssignments(eSolution, subMapping);
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

   public Solution transform(ESolution eSolution)
   {
      Solution result = new Solution();
      result.setModel(model);



      // top level
      for (ECompMapping eCompMap : eSolution.getCompMappings())
      {
         Assignment assignment = new Assignment();
         assignment.setRequest(findRequest(eCompMap.getRequName()));
         assignment.setTopLevel(true);
         result.addAssignment(assignment);
      }

      return result;
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
