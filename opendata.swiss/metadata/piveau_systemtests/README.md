# Testbench

This project is a collection of system tests for [piveau](https://www.piveau.io/) in the context of the [opendata.swiss](https://opendata.swiss) portal.

The services that make up the "system under test" are controlled by **docker-compose.yaml** file.

## Goal-driven test execution order

Test methods declare which goals they achieve and which goals they depend on. From this, the `GlobalTestRunner` determines the correct execution sequence (via topological sort). Running the resulting chain of tests incrementally "builds up" the state of the system.

For example:
- `createCatalog()` provides `Goal.CATALOG_CREATED`
- `createDataset()` depends on `Goal.CATALOG_CREATED` and provides `Goal.DATASET_CREATED`
- `updateDataset()` depends on `Goal.DATASET_CREATED` and provides `Goal.DATASET_UPDATED`

Annotations on test methods:
- `@Provides(Goal.CATALOG_CREATED)`: Marks that a test method achieves this goal.
- `@DependsOn(Goal.CATALOG_CREATED)`: Marks that a test method requires this goal.


## Test discovery

`GlobalTestRunner` scans the package `swiss.opendata.piveau.testbench.scenarios` for test methods.


## Prepare

Adjust `PIVEAU_HUB_SEARCH_API_KEY` in your environment to match with the one used in [TestConstants](src/test/java/swiss/opendata/piveau/testbench/TestConstants.java)


## Execution Modes

There are three execution modes:

1. Run all tests in topological order
2. Run a focused test and its dependencies (useful for reproducing failed tests)
3. Verify the dependency graph (DAG)

### Run all tests in topological order

```
mvn test
```

---

### Run a focused test and its dependencies (useful for reproducing failed tests)

To run a specific test method along with its required dependencies, use the `piveau.target.test` property. This filters the execution to only the necessary chain, ensuring valid state gets built up for the focused test

```
mvn test -Dpiveau.target.test=swiss.opendata.piveau.testbench.scenarios.simple.DatasetTest#createDataset
```

---

### Verify the dependency graph (DAG)

This "meta-test" runs every scenario method in a simulated focused run, to verify that the entire dependency graph is sound and that every test method declares sufficient dependencies to be run in focused mode.

Note: This runs the actual system tests multiple times (once for each scenario chain it is part of), so it is resource-intensive.

```
mvn test -Pdag-verification
```


## Report Generation

Command: `mvn allure:report`

Viewing: Run `mvn allure:serve` to start a local web server and auto-open the report.