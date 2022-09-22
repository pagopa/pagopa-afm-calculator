# run from this directory

# azure login
#az acr login -n pagopaucommonacr

# create containers
docker-compose up -d

# waiting the containers
printf 'Waiting for the service'
attempt_counter=0
max_attempts=10
until $(curl --output /dev/null --silent --head --fail http://localhost:8080/info); do
    if [ ${attempt_counter} -eq ${max_attempts} ];then
      echo "Max attempts reached"
      exit 1
    fi

    printf '.'
    attempt_counter=$(($attempt_counter+1))
    sleep 5
done
printf 'Service Started'

# run integration tests
cd src || exit
yarn install
yarn test
