package missionmodel;

import constraints.procedures.ActivityArgumentConstraint;
import constraints.procedures.DataCollectionOverlap;
import gov.nasa.ammos.aerie.procedural.constraints.Violation;
import gov.nasa.ammos.aerie.procedural.constraints.Violations;
import gov.nasa.ammos.aerie.procedural.scheduling.plan.EditablePlan;
import gov.nasa.ammos.aerie.procedural.scheduling.utils.DefaultEditablePlanDriver;
import gov.nasa.ammos.aerie.procedural.timeline.Interval;
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.DirectiveStart;
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults;
import gov.nasa.ammos.aerie.procedural.utils.TypeUtilsEditablePlanAdapter;
import gov.nasa.ammos.aerie.procedural.utils.TypeUtilsPlanAdapter;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.orchestration.simulation.SimulationUtility;
import gov.nasa.jpl.aerie.types.Plan;
import gov.nasa.jpl.aerie.types.Timestamp;
import missionmodel.generated.GeneratedModelType;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 * Example tests for procedural constraints using real simulation.
 * General workflow:
 * 1. Create a {@link SimulationUtility} instance.
 * 2. Load the mission model using the sim utility.
 * 3. Create a new empty plan. You'll need to use a couple adapters, see ConstraintProcedureTests.beforeEach.
 *    for an example.
 * 4. Add activities and simulate using the {@link EditablePlan} interface.
 * 5. Run the procedural constraint, collect violations and check results with assertions
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConstraintProcedureTests {

  private MissionModel<?> model;
  private SimulationUtility simUtility;
  private Instant simulationStartTime;
  private EditablePlan plan;

  /* Setup and instantiate the mission model. */
  @BeforeAll
  void beforeAll() {
    // Note no resource file streamer is required since we don't plan to write out resources to a file
    simUtility = new SimulationUtility();
    Configuration simConfig = Configuration.defaultConfiguration();
    simulationStartTime = Instant.parse("2025-01-01T00:00:00Z");
    model = SimulationUtility.instantiateMissionModel(new GeneratedModelType(), simulationStartTime, simConfig);

  }

  /* Close out . */
  @AfterAll
  void afterAll() {
    simUtility.close();
  }

  @BeforeEach
  void beforeEach() {
    plan = new DefaultEditablePlanDriver(
      new TypeUtilsEditablePlanAdapter(
        new TypeUtilsPlanAdapter(
          new Plan("TestPlan", new Timestamp(simulationStartTime), new Timestamp(simulationStartTime.plusSeconds(60 * 60 * 24)), Map.of(), Map.of())
        ),
        simUtility,
        model
      )
    );
  }

  @Test
  final void testDataCollectionOverlap() {
    // Create two overlapping data collection activities that overlap. These should overlap from the start of the
    // second activity to end of the first activity
    SerializedValue actDur = new DurationValueMapper().serializeValue(Duration.MINUTE);
    plan.create("CollectData", new DirectiveStart.Absolute(Duration.SECOND), Map.of("duration", actDur));
    plan.create("CollectData", new DirectiveStart.Absolute(Duration.MINUTE), Map.of("duration", actDur));

    // Simulate Plan
    SimulationResults simResults = plan.simulate();

    // Compute violations
    Violations violations = new DataCollectionOverlap().run(plan, simResults);

    // There should be one violation
    assertEquals(1, violations.collect().size());

    // Check the violation
    assertIterableEquals(
      List.of(
        new Violation(Interval.between(Duration.MINUTE, Duration.MINUTE.plus(Duration.SECOND)), null, List.of())
      ),
      violations.collect()
    );
  }

  @Test
  final void testActivityArgumentConstraint() {
    // Create single activity with out of bounds argument
    SerializedValue actDur = new DurationValueMapper().serializeValue(Duration.MINUTE);
    plan.create("CollectData", new DirectiveStart.Absolute(Duration.MINUTE), Map.of("duration", actDur, "rate", SerializedValue.of(15.0)));

    // Simulate Plan
    SimulationResults simResults = plan.simulate();

    // Compute violations
    Violations violations = new ActivityArgumentConstraint(10.0, 20.0).run(plan, simResults);

    // There should be one violation
    assertEquals(1, violations.collect().size());

    // Check the violation
    assertIterableEquals(
      List.of(
        new Violation(Interval.between(Duration.MINUTE, Duration.MINUTE.plus(Duration.MINUTE)), null, List.of())
      ),
      violations.collect()
    );
  }

  @Test
  final void testDataCollectionSeparation() {
    // Create single activity with out of bounds argument
    SerializedValue actDur = new DurationValueMapper().serializeValue(Duration.MINUTE);
    plan.create("CollectData", new DirectiveStart.Absolute(Duration.MINUTE), Map.of("duration", actDur, "rate", SerializedValue.of(15.0)));

    // Simulate Plan
    SimulationResults simResults = plan.simulate();

    // Compute violations
    Violations violations = new ActivityArgumentConstraint(10.0, 20.0).run(plan, simResults);

    // There should be one violation
    assertEquals(1, violations.collect().size());

    // Check the violation
    assertIterableEquals(
      List.of(
        new Violation(Interval.between(Duration.MINUTE, Duration.MINUTE.plus(Duration.MINUTE)), null, List.of())
      ),
      violations.collect()
    );
  }

}
