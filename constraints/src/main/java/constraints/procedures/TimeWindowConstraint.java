package constraints.procedures;

import gov.nasa.ammos.aerie.procedural.constraints.Constraint;
import gov.nasa.ammos.aerie.procedural.constraints.Violations;
import gov.nasa.ammos.aerie.procedural.constraints.annotations.ConstraintProcedure;
import gov.nasa.ammos.aerie.procedural.timeline.Interval;
import gov.nasa.ammos.aerie.procedural.timeline.collections.Windows;
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.AnyInstance;
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.Instance;
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan;
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

@ConstraintProcedure
public record TimeWindowConstraint(Duration startDur, Duration endDur) implements Constraint {

  @Override
  public Violations run(Plan plan, SimulationResults simResults) {
    Interval timeInt = Interval.between(startDur, endDur);

    List<Instance<AnyInstance>> acts  = simResults.instances("CollectData").collect();

    // Collector of violation windows
    Windows violationWins = new Windows();
    for (Instance<AnyInstance> act : acts) {
      Interval violation = timeInt.intersection(act.getInterval());
      if (!violation.isEmpty()) {
        violationWins = violationWins.union(new Windows(violation));
      }
    }
    return Violations.inside(violationWins);
  }
}


