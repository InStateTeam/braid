#!/bin/bash

pushd `dirname $0` > /dev/null
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
popd > /dev/null
echo ${SCRIPTPATH}

pushd "${SCRIPTPATH}/.."
./deploy/deploy.sh hermes-sample-server hermes-sample-server latest docker-registry-default.bluebank.io:443
popd
