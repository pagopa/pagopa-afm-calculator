# How run with Docker 🐳

To run the integration tests with docker, you can run from this directory the script:


``containerRegistry=pagopadcommonacr.azurecr.io sh ./run_integration_test.sh``

_Note_: for UAT and PROD ACR is **required** the login `az acr login -n <acr-name>`
