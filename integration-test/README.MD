# Integration Tests
👀 Integration tests are in `integration-test/src/` folder. See there for more information. 

## How run with Docker 🐳

To run the integration tests with docker, you can run from this directory the script:


``` shell
containerRegistry=pagopadcommonacr.azurecr.io sh ./run_integration_test.sh
```
With this command **you test the images on Azure ACR**

It is necessary to set `containerRegistry` env variable to specify the container registry.

ℹ️ _Note_: for PagoPa ACR is **required** the login `az acr login -n <acr-name>`



## Run your local image 💻
If you want to test your branch locally,
you can run integration test with your own image:

``` shell
sh ./run_integration_test.sh -local
```

With this command **you build a new docker image and test it**
