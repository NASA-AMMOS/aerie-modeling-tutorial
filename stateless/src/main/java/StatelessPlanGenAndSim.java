import gov.nasa.ammos.aerie.procedural.scheduling.utils.DefaultEditablePlanDriver;
import gov.nasa.ammos.aerie.procedural.utils.TypeUtilsEditablePlanAdapter;
import gov.nasa.ammos.aerie.procedural.utils.TypeUtilsPlanAdapter;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.orchestration.simulation.CanceledListener;
import gov.nasa.jpl.aerie.orchestration.simulation.ResourceFileStreamer;
import gov.nasa.jpl.aerie.orchestration.simulation.SimulationResultsWriter;
import gov.nasa.jpl.aerie.orchestration.simulation.SimulationUtility;
import gov.nasa.ammos.aerie.procedural.scheduling.plan.EditablePlan;
import gov.nasa.jpl.aerie.types.Plan;
import gov.nasa.jpl.aerie.types.Timestamp;
import missionmodel.Configuration;
import missionmodel.generated.GeneratedModelType;
import scheduling.procedures.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class StatelessPlanGenAndSim {

  public static void main(String[] args) {

    final var rfs = new ResourceFileStreamer();
    SimulationUtility simUtility = new SimulationUtility(rfs);

    final var simulationStartTime = Instant.parse("2025-01-01T00:00:00Z");
    Configuration simConfig = Configuration.defaultConfiguration();

    MissionModel<?> model = SimulationUtility.instantiateMissionModel(new GeneratedModelType(), simulationStartTime, simConfig);
    Plan plan =  new Plan("Demo Plan", new Timestamp(simulationStartTime), new Timestamp(simulationStartTime.plusSeconds(60 * 60 * 24)), Map.of(), Map.of());

    EditablePlan editablePlan = new DefaultEditablePlanDriver(
      new TypeUtilsEditablePlanAdapter(
        new TypeUtilsPlanAdapter(plan),
        simUtility,
        model
      )
    );

    int numObs = 4;
    Duration startTime = Duration.ZERO;
    Duration period = Duration.hours(2);
    new SchedulePeriodicDataCollections(startTime, period, numObs, Duration.MINUTE, 10.0 ).run(editablePlan);

    Future<SimulationResults> resultsFuture = simUtility.simulate(model, plan);

    System.out.println("Writing Results...");
    final var canceledListener = new CanceledListener();
    try {
      final var results = resultsFuture.get();
      final var resultsWriter = new SimulationResultsWriter(results, plan, rfs);
      resultsWriter.writeResults(canceledListener, Path.of("sim_results.json"));

    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    simUtility.close();

  }

}
