# TTC 2018: Case 1 "Quality-based Software-Selection and Hardware-Mapping as Model Transformation Problem"

## Getting started

In order to get the case working, perform the following steps:

- clone the repository: `git clone git@git-st.inf.tu-dresden.de:stgroup/ttc18.git && cd ttc18`
- build it: `./gradlew build` (or `gradlew.bat build` on Windows)
- run the benchmark: `./gradlew benchmarkFull`
	- as this might take long, running a single scenario is possible with `./gradlew benchmarkFull '-Pscenario=1'`

## Overview over the repository structure

All modules are prefixed with `jastadd-mquat`, as this is an implementation of MQuAT (Multi-Quality AutoTuning) based on [JastAdd](http://www.jastadd.org). There are 5 modules:

- `base`: Contains the specifications for grammar and attributes, (de-)serializers and the model generator
- `benchmark`: Benchmark infrastructure and settings
- `solver`: Interfaces for solvers, and a small testsuite
- `solver-ilp`: Reference implementation using ILP
- `solver-simple`: Naïve, brute-force solver written in Java

**TODO: Check links** https://git-st.inf.tu-dresden.de/stgroup/ttc18/blob/master/jastadd-mquat-benchmark/src/main/java/de/tudresden/inf/st/mquat/benchmark/BenchmarkableSolver.java

## Creating a solution

A new solution should be created using a new module (or multiple, if necessary). You can use the simple-solver module as an example.
The following steps need to be completed:

1. Create an implementation of [`de.tudresden.inf.st.mquat.solving.BenchmarkableSolver`](https://git-st.inf.tu-dresden.de/stgroup/ttc18/blob/master/jastadd-mquat-solver/src/main/java/de/tudresden/inf/st/mquat/solving/BenchmarkableSolver.java) (which extends the [`Solver`](https://git-st.inf.tu-dresden.de/stgroup/ttc18/blob/master/jastadd-mquat-solver/src/main/java/de/tudresden/inf/st/mquat/solving/Solver.java) interface). The main method here is `public Solution solve(Root model) throws SolvingException`, which takes a model as input an returns a solution
1. Add an include of your project to `settings.gradle`
1. Optional step: Create a test case by extending the [`HandwrittenTestSuite`](https://git-st.inf.tu-dresden.de/stgroup/ttc18/blob/master/jastadd-mquat-solver/src/test/java/de/tudresden/inf/st/mquat/solving/HandwrittenTestSuite.java)
1. Add a compile dependency to your project in `build.gradle` of the project `jastadd-mquat-benchmark`
1. Update [`de.tudresden.inf.st.mquat.benchmark.SolverFactory.createAvailableSolversIfNeeded`](https://git-st.inf.tu-dresden.de/stgroup/ttc18/blob/master/jastadd-mquat-benchmark/src/main/java/de/tudresden/inf/st/mquat/benchmark/SolverFactory.java#L22) to create a new instance of your solver
1. Add the name of your solver to the benchmark settings
	- use `jastadd-mquat-benchmark/src/main/resources/scenarios.json` for the Gradle task `benchmarkFull`
	- use `jastadd-mquat-benchmark/src/main/resources/local-benchmark-settings.json` for the Gralde task `benchmarkCustom` (see [Custom Benchmark](#custom-benchmark) for details)
1. Run the benchmark, either `./gradlew benchmarkFull` or `./gradlew benchmarkCustom`

## Custom Benchmark

To test your solution, the Gradle task `benchmarkCustom` can be used. This task generates a custom set of models and runs a benchmark for them.
All default parameters are specified in the file `benchmark-settings.json` within the directory `jastadd-mquat-benchmark/src/main/resources`.
To change them, create a new file in this directory named `local-benchmark-settings.json`.
In this local version, all parameter values override the default settings, but are ignored when committing.

To test your solver with the name `fancy-solver` along with the reference implementation using a model with `10` and `15` requests and a timeout of 50 seconds, the file `local-benchmark-settings.json` would be as follows.

```json
{
  "solvers": [
    "ilp-direct",
    "fancy-solver"
  ],
  "basic": {
    "verbose": true,
    "minRequests": 10,
    "maxRequests": 15,
    "stepRequests": 5,
    "timeoutValue": 50,
    "timeoutUnit": "SECONDS",
    "total": 2
  }
}
```

The value `total` is used to constrain the total number of models to be generated. Set this to `null` (the default) to generate all value for the defined parameter ranges.
Refer to [`de.tudresden.inf.st.mquat.generator.ScenarioDescription`](https://git-st.inf.tu-dresden.de/stgroup/ttc18/blob/master/jastadd-mquat-base/src/main/java/de/tudresden/inf/st/mquat/generator/ScenarioDescription.java) for a description of the possible parameters.

## Notes and Troubleshooting

- please use the gradle wrapper script, as different version of Gradle might not work with the setup
	- the wrapper script uses version `3.3`
- if anything is not working as expected, feel free to contact on of the authors of the TTC case or open an [issue](https://git-st.inf.tu-dresden.de/stgroup/ttc18/issues/new)
