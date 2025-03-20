#!/bin/bash

if [[ "$(pwd)" =~ .*"openapi".* ]]; then
    cd ..
fi

mvn test -Dtest=OpenApiGenerationTest

jq '.paths."/fees" = .paths."/fees/multi" | del( .paths."/fees/multi")' ./openapi/openapi-v2.json > ./openapi/temp.json.temp && mv ./openapi/temp.json.temp ./openapi/openapi-v2.json
jq '.paths."/psps/fees/{idPsp}" = .paths."/psps/{idPsp}/fees/multi" | del( .paths."/psps/{idPsp}/fees/multi")' ./openapi/openapi-v2.json > ./openapi/temp.json.temp && mv ./openapi/temp.json.temp ./openapi/openapi-v2.json