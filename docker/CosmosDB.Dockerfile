# https://github.com/Azure/azure-cosmos-db-emulator-docker/issues/60

FROM mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest

RUN /bin/bash -c "printf '\x00' | dd of=/usr/local/bin/cosmos/cosmosdb-emulator conv=notrunc bs=1 seek=$((0x12eea2))"
