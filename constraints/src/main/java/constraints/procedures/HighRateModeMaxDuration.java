package constraints.procedures;

import gov.nasa.ammos.aerie.procedural.constraints.Constraint;
import gov.nasa.ammos.aerie.procedural.constraints.Violations;
import gov.nasa.ammos.aerie.procedural.constraints.annotations.ConstraintProcedure;
import gov.nasa.ammos.aerie.procedural.timeline.collections.Windows;
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Strings;
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan;
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

@ConstraintProcedure
public record HighRateModeMaxDuration(Duration maxDur) implements Constraint {
  @Override
  public Violations run(Plan plan, SimulationResults simResults) {
    final var mode = simResults.resource("MagDataMode", Strings.deserializer() );
    Windows highRateWins = mode.highlightEqualTo("HIGH_RATE").filterLongerThan(maxDur);
    return Violations.inside(new Windows(highRateWins));
  }
}
