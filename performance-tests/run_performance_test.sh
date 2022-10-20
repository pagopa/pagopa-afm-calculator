# sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey>

ENV=$1
TEST_TYPE=$2
SCRIPT=$3
DB_NAME=$4
API_SUBSCRIPTION_KEY=$5

if [ -z "$ENV" ]
then
  echo "No env specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey>"
  exit 1
fi

if [ -z "$TEST_TYPE" ]
then
  echo "No test type specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey>"
  exit 1
fi
if [ -z "$SCRIPT" ]
then
  echo "No script name specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey>"
  exit 1
fi

if [ -z "$DB_NAME" ]
then
  DB_NAME="k6"
  echo "No DB name specified: 'k6' is used."
fi

export ENV=${ENV}
export TEST_TYPE=${TEST_TYPE}
export SCRIPT=${SCRIPT}
export DB_NAME=${DB_NAME}
export API_SUBSCRIPTION_KEY=${API_SUBSCRIPTION_KEY}

stack_name=$(cd .. && basename "$PWD")
docker-compose -p "${stack_name}" up -d --remove-orphans --force-recreate
