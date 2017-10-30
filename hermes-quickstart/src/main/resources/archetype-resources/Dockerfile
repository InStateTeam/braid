FROM java:8-jdk-alpine
RUN mkdir -p /home/${artifactId}
RUN chgrp -R 0 /home/${artifactId} && chmod -R g+rwX /home/${artifactId}
ADD ./target/${artifactId}.jar /home/${artifactId}/bin/${artifactId}.jar

EXPOSE 8080
USER 999
#ENTRYPOINT ["ls"]
WORKDIR /home/${artifactId}
ENTRYPOINT ["/usr/bin/java", "-jar", "bin/${artifactId}.jar"]