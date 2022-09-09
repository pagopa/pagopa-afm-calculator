#!/bin/bash
curl http://localhost:8586/v3/api-docs | python3 -m json.tool > ./openapi.json
python3 generate_openapi_node.py ./openapi.json
# UI mode http://localhost:8586/swagger-ui/index.html
