#!/bin/sh

exec java -jar /opt/braid/braid-server.jar ${NODE_RPC_ADDRESS} ${NODE_RPC_USERNAME} ${NODE_RPC_PASSWORD} ${PORT} ${OPEN_API_VERSION} ${CORDAPP_DIRECTORY}
