package scheduling.procedures;

import gov.nasa.ammos.aerie.procedural.scheduling.Goal;
import gov.nasa.ammos.aerie.procedural.scheduling.plan.EditablePlan;
import gov.nasa.ammos.aerie.procedural.scheduling.annotations.SchedulingProcedure;
import gov.nasa.ammos.aerie.procedural.scheduling.plan.NewDirective;
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.AnyDirective;
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.DirectiveStart;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

@SchedulingProcedure
public record SchedulePeriodicDataCollections(Duration startTime, Duration period, int numObs, Duration actDur, double rate) implements Goal {
  @Override
  public void run(EditablePlan plan) {
    var currentTime = startTime;

    Map<String, SerializedValue> actArgs;
    NewDirective newDirective;
    for (var i = 0; i < numObs; i++) {

      // Set activity arguments
      actArgs = Map.of(
        "duration", new DurationValueMapper().serializeValue(actDur),
        "rate", SerializedValue.of(rate));

      // Create new activity
      newDirective = new NewDirective(
        new AnyDirective(actArgs),
        "CollectData",
        "CollectData",
        new DirectiveStart.Absolute(currentTime));

      // Put new activity in the plan
      plan.create(newDirective);

      currentTime = currentTime.plus(period);
    }
    plan.commit();
  }
}
