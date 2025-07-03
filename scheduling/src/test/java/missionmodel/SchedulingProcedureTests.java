package missionmodel;

import gov.nasa.ammos.aerie.procedural.scheduling.plan.EditablePlan;
import gov.nasa.ammos.aerie.procedural.scheduling.utils.DefaultEditablePlanDriver;
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults;
import gov.nasa.ammos.aerie.procedural.utils.TypeUtilsEditablePlanAdapter;
import gov.nasa.ammos.aerie.procedural.utils.TypeUtilsPlanAdapter;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.orchestration.simulation.SimulationUtility;
import gov.nasa.jpl.aerie.types.Plan;
import gov.nasa.jpl.aerie.types.Timestamp;
import missionmodel.generated.GeneratedModelType;
import org.junit.jupiter.api.*;
import scheduling.procedures.SchedulePeriodicDataCollections;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Example test for procedural scheduling
 * General workflow:
 * 1. Create a {@link SimulationUtility} instance.
 * 2. Load the mission model using the sim utility.
 * 3. Create a new empty plan. You'll need to use a couple adapters, see SchedulingProcedureTests.beforeEach.
 *    for an example.
 * 4. Add activities via scheduling procedures - simulate before/after as necessary
 * 5. Look at resultant plan and check it with assertions
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchedulingProcedureTests {

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
  final void testSchedulePeriodicDataCollections() {

    // Simulate Plan if necessary
    SimulationResults simResults = plan.simulate();

    // Run Scheduler
    int numObs = 4;
    Duration startTime = Duration.ZERO;
    Duration period = Duration.hours(2);
    new SchedulePeriodicDataCollections(startTime, period, numObs, Duration.MINUTE, 10.0 ).run(plan);

    // The number of activities should match what was requested in the scheduler
    assertEquals(numObs, plan.directives("CollectData").collect().size() );

    // Check that the period is as expected
    Duration currentTime = startTime;
    for (var actDirective : plan.directives("CollectData").collect()) {
      assertEquals(currentTime, actDirective.getStartTime());
      currentTime = currentTime.plus(period);
    }
  }

}
