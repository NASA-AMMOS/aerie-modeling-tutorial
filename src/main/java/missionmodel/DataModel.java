package missionmodel;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Reactions;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockEffects;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.asPolynomial;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

public class DataModel {

    public MutableResource<Discrete<Double>> RecordingRate; // Megabits/s

    public MutableResource<Discrete<MagDataCollectionMode>> MagDataMode;

    public Resource<Discrete<Double>> MagDataRate; // bps

    public MutableResource<Discrete<Double>> SSR_Volume; // Gigabits

    public MutableResource<Discrete<Double>> SSR_Volume_MultiStep; // Gigabits

    public MutableResource<Discrete<Double>> SSR_Volume_UponRateChange; // Gigabits

    public Double previousRecordingRate;

    public MutableResource<Discrete<Double>> SSR_Volume_Sampled; // Gigabits

    private MutableResource<Clock> TimeSinceLastRateChange = (MutableResource<Clock>) clock();

    private final Duration INTEGRATION_SAMPLE_INTERVAL = Duration.duration(60, Duration.SECONDS);

    public Resource<Polynomial> SSR_Volume_Polynomial;  // Gigabits

    private final Double SSR_MAX_CAPACITY = 100.0; // Gigabits

    public Resource<Polynomial> RecordingRate_UnitAware;  // Mbps
    public Resource<Polynomial> SSR_Volume_UnitAware;  // Gigabits

    public DataModel(Registrar registrar) {
        RecordingRate = resource(discrete(0.0));
        registrar.discrete("RecordingRate", RecordingRate, new DoubleValueMapper());

        MagDataMode = resource(discrete(MagDataCollectionMode.OFF));
        registrar.discrete("MagDataMode",MagDataMode, new EnumValueMapper<>(MagDataCollectionMode.class));

        MagDataRate = map(MagDataMode, MagDataCollectionMode::getDataRate);
        registrar.discrete("MagDataRate", MagDataRate, new DoubleValueMapper());

        previousRecordingRate = 0.0;

        //
        // Integration Method 1 - Accumulate all volume at the end of the activity
        //
        SSR_Volume = resource(discrete(0.0));
        registrar.discrete("SSR_Volume", SSR_Volume, new DoubleValueMapper());

        //
        // Integration Method 2 - Spread out accumulation over many steps
        //
        SSR_Volume_MultiStep = resource(discrete(0.0));
        registrar.discrete("SSR_Volume_MultiStep", SSR_Volume_MultiStep, new DoubleValueMapper());

        //
        // Integration Method 3 - Accumulate data volume upon change to recording rate
        //
        SSR_Volume_UponRateChange = resource(discrete(0.0));
        registrar.discrete("SSR_Volume_UponRateChange", SSR_Volume_UponRateChange, new DoubleValueMapper());
        // Alternate Approach
        // Reactions.wheneverUpdates(RecordingRate, this::uponRecordingRateUpdate);

        //
        // Integration Method 4 - Sample-based integration
        //
        SSR_Volume_Sampled = resource(discrete(0.0));
        registrar.discrete("SSR_Volume_UponRateChange", SSR_Volume_UponRateChange, new DoubleValueMapper());

        //
        // Integration Method 5 - Integrated resource
        //
        // Approach 1 - Simple Integrated Resource
        SSR_Volume_Polynomial = PolynomialResources.integrate(asPolynomial(this.RecordingRate), 0.0);
        registrar.real( "SSR_Volume_Polynomial", PolynomialResources.assumeLinear(SSR_Volume_Polynomial));

        // Approach 2 - Integral with min/max bounds
//        var clampedIntegrate = PolynomialResources.clampedIntegrate( asPolynomial(this.RecordingRate),
//          PolynomialResources.constant(0.0),
//          PolynomialResources.constant(SSR_MAX_CAPACITY),
//          0.0);
//        SSR_Volume_Polynomial = clampedIntegrate.integral();

        //
        // Unit Aware Resources
        //
    }

    public void uponRecordingRateUpdate() {
        // Determine time elapsed since last update
        Duration t = currentValue(TimeSinceLastRateChange);
        // Update volume only if time has actually elapsed
        if (!t.isZero()) {
            DiscreteEffects.increase(this.SSR_Volume_UponRateChange, previousRecordingRate * t.ratioOver(Duration.SECONDS) / 1000.0);
        }
        previousRecordingRate = currentValue(RecordingRate);
        // Restart clock (set back to zero)
        ClockEffects.restart(TimeSinceLastRateChange);
    }

    public void increaseRecordingRate(Double rateIncrease) {
        // Set rate to new rate
        DiscreteEffects.increase(RecordingRate, rateIncrease);

        // Update data volume to reflect volume since last update
        uponRecordingRateUpdate();

    }

    // Integrate data volume in the SSR_Volume by sampling the value of the recording rate at a fixed interval
    // This implementation uses the "right" Riemann sum approach to numerical integration, but could easily
    // be modified to another method such as the Trapezoid rule by storing off the previous value of the
    // recording rate at each sample.
    public void integrateSampledSSR() {
        while(true) {
            Duration dt = currentValue(TimeSinceLastRateChange);
            Double currentRecordingRate = currentValue(RecordingRate);
            DiscreteEffects.increase(SSR_Volume_Sampled, currentRecordingRate * dt.ratioOver(Duration.SECONDS) / 1000.0);
            ClockEffects.restart(TimeSinceLastRateChange);
            delay(INTEGRATION_SAMPLE_INTERVAL);
        }
    }

}
