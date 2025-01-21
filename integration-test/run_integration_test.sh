#!/bin/bash

# example: sh ./run_integration_test.sh <local|dev|uat|prod> <old|allure>
set -e

ENVIRONMENT=$1
TYPE=$2

# run integration tests
cd ./src || exit
yarn install
yarn add @azure/data-tables
yarn test-"$TYPE":"$ENVIRONMENT"

# generate allure report
if [ "$2" == "allure" ]
then
  cd ../
  allure generate ./allure/results -o ./allure/reports --clean
fi