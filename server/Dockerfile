FROM java:8-jdk-alpine
RUN mkdir hermes
RUN chgrp -R 0 hermes && chmod -R g+rwX hermes
ADD ./target/hermes-server.jar hermes/hermes-server.jar
EXPOSE 8080
USER 999
#ENTRYPOINT ["ls"]
WORKDIR hermes
ENTRYPOINT ["/usr/bin/java", "-jar", "hermes-server.jar"]
