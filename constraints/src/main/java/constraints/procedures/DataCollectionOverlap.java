package constraints.procedures;

import gov.nasa.ammos.aerie.procedural.constraints.Constraint;
import gov.nasa.ammos.aerie.procedural.constraints.Violations;
import gov.nasa.ammos.aerie.procedural.constraints.annotations.ConstraintProcedure;
import gov.nasa.ammos.aerie.procedural.timeline.Interval;
import gov.nasa.ammos.aerie.procedural.timeline.collections.Windows;
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan;
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults;

import java.util.ArrayList;
import java.util.List;

@ConstraintProcedure
public record DataCollectionOverlap() implements Constraint {
  @Override
  public Violations run(Plan plan, SimulationResults simResults) {
    // Get all collect data activities in a list
    var collectDataActs = simResults.instances("CollectData").collect();

    // Collector if violation intervals
    List<Interval> violationWins = new ArrayList<>();

    // Assuming the collected activity spans are in increasing time order,
    // loop through the activities looking for activity intersections.
    for (int ii = 0; ii < collectDataActs.size(); ii++) {
      Interval win = collectDataActs.get(ii).getInterval();
      for (int jj = ii; jj < collectDataActs.size(); jj++) {
        Interval tempWin = win.intersection(collectDataActs.get(jj).getInterval());
        // Collect intersecting interval, which is considered a violation
        // If no intersection is found, we know no more intersections should exist
        // between activities with later start times, so we can be done with this loop
        if (!tempWin.isEmpty()) {
          violationWins.add(tempWin);
        } else {
          break;
        }
      }
    }
    return Violations.inside(new Windows(violationWins));
  }
}
