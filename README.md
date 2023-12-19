# Mission Modeling Tutorial

Welcome Aerie modeling padawans! For your training today, you will be learning the basics of mission modeling in Aerie by building your own simple model of an on-board spacecraft solid state recorder. Through the process of building this model, you'll learn about the fundamental objects of a model, activities and resources, and their structure. You'll be introduced to the different categories of resources and learn how you define and implement each along with restrictions on when you can/can't modify them. As a bonus, we will also cover how you can make your resources "unit aware" to prevent those pesky issues that come along with performing unit conversions and how you can test your model without having to pull your model into an Aerie deployment.

Let the training begin!

This repo houses the simple on-board spacecraft solid state recorder model that you will build as part of Aerie's modeling tutorial.
While we recommend you go through the tutorial, this model also serves as an example you can use to jump start your own modeling efforts.
The model includes a resource that tracks recording rate and few different resources that show different ways in which you can integrate rate to get data volume over time.
A couple of different style data collection activities are also included to trigger recording rate changes.

## Outline

- Make sure you have access to an Aerie deployment (see FastTrack for a quick local install)

- Create new repo with mission model template (i.e. follow mission model template instructions) and make sure it compiles

  - With David's new framework, we should update this to account for his register in the Mission class
  - Also will need to update dependencies in build.gradle
  - Potentially suggest not including any activities/resources in the template...

- Create RecordingRate resource in a DataModel class
  - This will be a discrete resource
    - Briefly provide overview of a discrete resource
    - Note we will be talking about polynomial resources later
  - Register resource to UI

```java
package missionmodel;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

public class DataModel {

    public MutableResource<Discrete<Double>> RecordingRate; // Megabits/s

    public DataModel(Registrar registrar) {
        RecordingRate = resource(discrete(0.0));
        registrar.discrete("RecordingRate", RecordingRate, new DoubleValueMapper());
    }
}
```

- Create simple CollectData activity for a camera
  - 2 parameters (duration, rate)
  - Show simple approach to changing rate (increase/delay/decrease)
    - Briefly talk about effects, non-consumable vs. consumable, why using "set" is not the best option
    - Note the non-consumable approach that could hae been used to produce the same result (using clause)
  - Make sure to note how the package-info has to be updated

```java
package missionmodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("CollectData")
public class CollectData {

    @Export.Parameter
    public double rate = 10.0; // Mbps

    @Export.Parameter
    public Duration duration = Duration.duration(1, Duration.HOURS);

    @ActivityType.EffectModel
    public void run(Mission model) {

        /*
         Collect data at fixed rate over duration of activity
        */
        // Approach 1 - Modify rate at start/end of activity
        DiscreteEffects.increase(model.dataModel.RecordingRate, this.rate);
        delay(duration);
        DiscreteEffects.decrease(model.dataModel.RecordingRate, this.rate);

    }
}
```

```java
// Approach 2 - Non-consumable "using" approach
DiscreteEffects.using(model.dataModel.RecordingRate, -this.rate, () -> delay(duration) );
```

- Compile and load model into Aerie for a first look
  - Create a couple collect data activities (maybe overlapping) to see more interesting effect
  - Simulate
  - ![Tutorial Plan 1](docs/Tutorial_Plan_1.png)
- Create a second resource to tracks different data collection modes for a magnetometer that continuously collects data

  - Create enumeration class that maps mode to data rate

```java
package missionmodel;

public enum MagDataCollectionMode {
    OFF(0.0), // kbps
    LOW_RATE(500.0), // kbps
    HIGH_RATE(5000.0); // kbps

    private final double magDataRate;

    MagDataCollectionMode(double magDataRate) {
        this.magDataRate = magDataRate;
    }

    public double getDataRate() {
        return magDataRate;
    }
}
```

- Create discrete state resource

```java
public MutableResource<Discrete<MagDataCollectionMode>> MagDataMode;
```

```java
MagDataMode = resource(discrete(MagDataCollectionMode.OFF));
registrar.discrete("MagDataMode",MagDataMode, new EnumValueMapper<>(MagDataCollectionMode.class));
```

- Create simple activity, ChangeMagMode, to change instrumentB mode, which in turn will change its data rate
  - This shows how you can get the current value of resource and use it for computation
- Introduce a derived resource for showing just the mag data collection rate instead of the total recording rate
- Compile and load the model into Aerie again for a second look
  - Put both types of activities in plan and see how it changes the two rate resources and how mode is tracked
  - ![Tutorial Plan 2](docs/Tutorial_Plan_2.png)
- Create SSR volume resource

  - Talk about the various methods for integrating
  - Method 1 - Increase volume at end of activity

  - Method 2 - Increase volume across fixed number of steps within the activity

  - Note why these methods get more challenging with a mode based approach (integral is being tracking in the activity class and therefore activity needs to get track of the time since the mode changed, which isn't really something an activity should know/care about)

  - Method 3 - Reaction based approach

  - Method 4 - Daemon approach

  - Method 5 - Polynomial resource

- Create downlink activity that decreases recording rate at some point for more interesting looking plots

- How to show decomposition?? Maybe a calibration that decomposes into CollectData?

- Update Rate/SSR_Volume to Unit Aware Resources

- Show setting up tests (unit/simulation)

- Simple validation check (max collection rate?)
