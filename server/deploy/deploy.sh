#!/bin/bash
#
# Copyright (C) 2017 Bluebank.
# Author: Salim Badakhchani <salim.badakhchani@bluebank.io>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
########################################################################

# Declare command line varibles
PROJECT="$1"
IMAGE="$2"
TAG="$3"
DOMAIN="bluebank.io"
REGISTRY="docker-registry-default.${DOMAIN}:443"

# Usage options and user arguments
read -d '' USAGE << EOF
Usage: ./deploy.sh [option] <arg>

Example: ./deploy.sh <openshift project> <docker image> <docker tag> <docker registry>
Working example: ./deploy.sh xxx-nonroot-nginx-dev nonroot-nginx latest docker-registry-default.bluebank.io:443
EOF


# The deploy function checks for the existing project before deploying a clean build from scratch
deploy() {
PROJECTS="$(oc get projects)"
for project in $PROJECTS; do
	if [ "$project" == "${PROJECT}" ]; then
		oc delete project ${PROJECT} > /dev/null 2>&1
		until oc new-project ${PROJECT} > /dev/null 2>&1; do
			echo -e "Trying to re-provison project...Please be patient!"
			sleep 10
		done

	fi
done

oc new-project ${PROJECT} > /dev/null 2>&1
docker login --username=$(oc whoami) --password=$(oc whoami -t) ${REGISTRY}
docker pull ${IMAGE}
docker tag ${IMAGE} ${REGISTRY}/${PROJECT}/${IMAGE}:${TAG}
docker build -t ${REGISTRY}/${PROJECT}/${IMAGE}:${TAG} .
docker push ${REGISTRY}/${PROJECT}/${IMAGE}:${TAG}
oc new-app ${IMAGE}:${TAG}
oc delete service ${IMAGE}
oc create service nodeport ${IMAGE} --tcp=443:8080
oc create route edge --hostname=${PROJECT}.${DOMAIN} --service=${IMAGE} --port=8080 --insecure-policy=Redirect

}

if [[ $# < 1 ]]; then echo "${USAGE}" && exit; fi
while [[ $# > 0 ]]; do OPTS="$1"; shift
done

deploy