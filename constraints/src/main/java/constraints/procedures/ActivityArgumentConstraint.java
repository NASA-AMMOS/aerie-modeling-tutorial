package constraints.procedures;

import gov.nasa.ammos.aerie.procedural.constraints.Constraint;
import gov.nasa.ammos.aerie.procedural.constraints.Violations;
import gov.nasa.ammos.aerie.procedural.constraints.annotations.ConstraintProcedure;
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan;
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults;

@ConstraintProcedure
public record ActivityArgumentConstraint(double minValue, double maxValue) implements Constraint {
  @Override
  public Violations run(Plan plan, SimulationResults simResults) {
     return Violations.on(
       simResults.instances("CollectData").filter(false,
         $ -> (
           $.inner.arguments.get("rate").asReal().get() >= minValue &&
           $.inner.arguments.get("rate").asReal().get() <= maxValue)).active(),
       true);
  }
}
