FROM java:8-jdk-alpine
ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/hermes/hermes-server.jar"]
ADD target/hermes-server.jar /usr/share/hermes/hermes-server.jar
EXPOSE 8080