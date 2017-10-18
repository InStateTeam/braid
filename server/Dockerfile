FROM java:8-jdk-alpine
RUN mkdir -p /home/hermes
RUN chgrp -R 0 /home/hermes && chmod -R g+rwX /home/hermes
ADD ./target/hermes-server.jar /home/hermes/bin/hermes-server.jar
EXPOSE 8080
USER 999
#ENTRYPOINT ["ls"]
WORKDIR /home/hermes
ENTRYPOINT ["/usr/bin/java", "-jar", "bin/hermes-server.jar"]