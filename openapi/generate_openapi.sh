#!/bin/bash

# create containers
cd ../docker || exit
sh ./run_docker.sh "$1"

cd ../openapi || exit
curl http://localhost:8080/v3/api-docs | python3 -m json.tool > ./openapi.json
python3 generate_openapi_node.py ./openapi.json

# UI mode http://localhost:8585/swagger-ui/index.html
