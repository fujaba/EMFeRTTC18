# Setting up a benchmark

The default settings file is found in `src/main/resources/benchmark-settings.json`.
**However**, to change values, a separate file `src/main/resources/local-benchmark-settings.json` should be used.
This file is not under version control on purpose to allow local changes.

There, the following things can be specified. Listed are keys, possible prefixes (min, max, step) and some explanations.

- kind: either `incremental` for IncrementalBenchmark, or `basic` for Benchmark
- path: result path
- filePattern: pattern for result name, must contain one `%s` replaced by a timestamp
- solvers: a list of solvers to use (identified by their name)
- logLevel: level of logging (from Log4j) set for the package of each used solver
- basic
  - TopLevelComponents (min, max)
  - AvgNumSubComponents (min, max)
  - SubComponentDerivation (min, max)
  - NumImplementations (min, max)
  - NumModes (min, max)
  - Cpus (min, max)
  - Requests  (min, max, step) 
  - ResourceRatio  (min, max, step)
  - timeoutValue: value for timeout
  - timeoutUnit: unit for timeout
  - seed: input for random generator
  - total: total number of runs (useful to test the settings, but not run the full benchmark)
- incremental
  - requestsToChange
  - percentToChange

As an example, to set the minimum number of top level components to 5, the following entry is needed:

```json
{
  "kind": "normal",
  "basic": {
    "minTopLevelComponents": 5
  }
}
```
