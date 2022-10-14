# sh ./run_docker.sh <local|dev|uat|prod>

ENV=$1

if [ -z "$ENV" ]
then
  ENV="local"
  echo "No environment specified: local is used."
fi


if [ "$ENV" = "local" ]; then
  containerRegistry="pagopadcommonacr.azurecr.io"
  image="service-local:latest"
  echo "Running local image and dev dependencies"
else

  if [ "$ENV" = "dev" ]; then
    containerRegistry="pagopadcommonacr.azurecr.io"
    echo "Running all dev images"
  elif [ "$ENV" = "uat" ]; then
    containerRegistry="pagopaucommonacr.azurecr.io"
    echo "Running all uat images"
  elif [ "$ENV" = "prod" ]; then
    containerRegistry="pagopapcommonacr.azurecr.io"
    echo "Running all prod images"
  else
    echo "Error with parameter: use <local|dev|uat|prod>"
    exit 1
  fi

  pip3 install yq
  repository=$(yq -r '."microservice-chart".image.repository' ../helm/values-$ENV.yaml)
  image="${repository}:latest"
fi


export containerRegistry=${containerRegistry}
export image=${image}

stack_name=$(cd .. && basename "$PWD")
docker-compose -p "${stack_name}" up -d --remove-orphans --force-recreate
