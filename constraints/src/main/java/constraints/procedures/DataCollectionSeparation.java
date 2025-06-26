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

import java.util.ArrayList;
import java.util.List;

@ConstraintProcedure
public record DataCollectionSeparation(Duration minSeparation) implements Constraint {
  @Override
  public Violations run(Plan plan, SimulationResults simResults) {
    List<Instance<AnyInstance>> acts  = simResults.instances("CollectData").collect();

    List<Interval> actIntervals = new ArrayList<>();
    for ( Instance<AnyInstance> act : acts) {
      actIntervals.add(act.getInterval());
    }

    // Collector of violation windows
    Windows violationWins = new Windows();

    // For each activity, create an interval by adding minSeparation to each side
    // of the activity. The intersection of that interval with other activities
    // violate this separation constraint
    for (int i = 0; i < actIntervals.size(); i++) {
        Interval activity = actIntervals.get(i);
        Interval sepInterval = Interval.between(activity.start.minus(minSeparation), activity.end.plus(minSeparation));
        // Remove this activity before doing interval logic against all activities
        List<Interval> tmpActs = new ArrayList<>(actIntervals);
        tmpActs.remove(i);
        Windows actsMinusThisAct = new Windows(tmpActs);
        Windows tmpViolations = actsMinusThisAct.intersection(new Windows(sepInterval));
        // Collect violations
        violationWins = violationWins.union(tmpViolations);
    }
    return Violations.inside(violationWins);
  }
}
