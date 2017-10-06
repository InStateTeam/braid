#!/bin/sh
REP=hermes
IMG=hermes-server
TAG=latest
REG=docker-registry-default.bluebank.io:443
PORT=8080

#docker build -t ${REG}/${REP}/${IMG}:${TAG} .

docker login --username=$(oc whoami) --password=$(oc whoami -t) ${REG}
docker push ${REP}/${IMG}:${TAG}
#oc new-app ${IMG}
#oc delete service ${IMG}
# oc create service nodeport ${IMG} --tcp=443:8080
#oc create route edge --hostname=${IMG}.bluebank.io --service=${IMG} --path=/ --port=80d80 --insecure-policy=Redirect