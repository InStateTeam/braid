FROM java:8-jdk-alpine
USER 999
ENTRYPOINT ["/usr/bin/java", "-jar", "hermes/hermes-server.jar"]
RUN mkdir ~/hermes && cd ~/hermes
ADD ./target/hermes-server.jar hermes-server.jar
EXPOSE 8080

