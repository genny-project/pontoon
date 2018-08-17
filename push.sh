#!/bin/bash

if [ -z "${1}" ]; then
   version="latest"
else
   version="${1}"
fi


docker push gennyproject/bridge:"${version}"
docker tag  gennyproject/bridge:"${version}"  gennyproject/bridge:latest
docker push gennyproject/bridge:latest

