# Integration Test with Cucumber

## Technology Stack

- [cucumber js](https://github.com/cucumber/cucumber-js)
- NodeJS v14.17.6

## How to start

- install dependencies: `yarn install`
- run tests: `yarn test`

if all right you should see something like that :

```sh
16 scenarios (16 passed)
78 steps (78 passed)
0m09.409s (executing steps: 0m09.349s)
┌──────────────────────────────────────────────────────────────────────────┐
│ View your Cucumber Report at:                                            │
│ https://reports.cucumber.io/reports/16ebc4c0-cab6-41f6-9355-f894f9a9601d │
│                                                                          │
│ This report will self-destruct in 24h.                                   │
│ Keep reports forever: https://reports.cucumber.io/profile                │
└──────────────────────────────────────────────────────────────────────────┘
```

Click on reporter link to view details .

### Debug

To run a single _feature_ or single _Scenario_ typing

Ex. single _features_ `organizations.feature`
```sh
npx cucumber-js -r step_definitions features/<filename>.feature
```

Ex. single _Scenario_ into `<filename>.feature` ( add source line )
```sh
npx cucumber-js -r step_definitions features/<filename>.feature:46
```

### Note

Please, before starting the tests, remember to:
1. if you are in local environment then create the issuerrange table;
2. if you are in local environment then import test data from `./config/ISSUER_RANGE_for_test.csv` file into the issuerrange table;
3. configure the subkey environment variable (for the local environment it can take on any value);
4. start the Backend.

You can configure the host in `./config/.env.*` file.

