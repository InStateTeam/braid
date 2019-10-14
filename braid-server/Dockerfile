FROM azul/zulu-openjdk:8

LABEL VENDOR="Bluebank / Cordite" MAINTAINER="devops@cordite.foundation"

EXPOSE 8080

ENV NODE_RPC_ADDRESS="localhost"
ENV NODE_RPC_USERNAME="user1"
ENV NODE_RPC_PASSWORD="password"
ENV PORT=8080
ENV OPEN_API_VERSION=3
ENV CORDAPP_DIRECTORY=/opt/braid/cordapps

WORKDIR /opt/braid

RUN mkdir -p /opt/braid /home/braid \
  && groupadd -g 1000 -r braid \
  && useradd -r -u 1000 -g braid braid \
  && chgrp -R 0 /opt/braid \
  && chmod -R g=u /opt/braid \
  && chown -R braid:braid /opt/braid /home/braid

USER braid

VOLUME /opt/braid/cordapps

COPY target/braid-server.jar /opt/braid/braid-server.jar

CMD ["/usr/bin/java", "-jar", "/opt/braid/braid-server.jar", "${NODE_RPC_ADDRESS}", "${NODE_RPC_USERNAME}", "${NODE_RPC_PASSWORD}", "${PORT}", "${OPEN_API_VERSION}", "${CORDAPP_DIRECTORY}"]
