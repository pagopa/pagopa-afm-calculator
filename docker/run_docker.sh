ENV=$1

if [ -z "$ENV" ]
then
  ENV="local"
  echo "no environment specified: local is used."
fi


if [ "$ENV" = "local" ]; then
  containerRegistry=pagopadcommonacr.azurecr.io
  image="service-local:latest"
  echo "Running local image and dev dependencies"
else
  pip3 install yq
  repository=$(yq -r '."microservice-chart".image.repository' ../helm/values-$ENV.yaml)
  image="${repository}:latest"
fi

if [ "$ENV" = "dev" ]; then
  containerRegistry=pagopadcommonacr.azurecr.io
  echo "Running all dev images"
fi
if [ "$ENV" = "uat" ]; then
  containerRegistry=pagopaucommonacr.azurecr.io
  echo "Running all uat images"
fi
if [ "$ENV" = "prod" ]; then
  containerRegistry=pagopapcommonacr.azurecr.io
  echo "Running all prod images"
fi

export containerRegistry=${containerRegistry}
export image=${image}

stack_name=$(cd .. && basename "$PWD")
docker-compose -p "${stack_name}" up -d --remove-orphans --force-recreate
