# Welcome to `${artifactId}` project!

<h2>Building</h2>

```bash
mvn clean install  
```

<h2>Running locally</h2>

Either via an IDE or command line:

```bash
java -jar target/${artifactId}.jar
```

<h2>Deploying to OpenShift</h2>

Requirements:

0. Have OC installed using the Redhat account setup [here](https://gitlab.bluebank.io/devops/Infra)
1. Log into OC using your token from [here](https://openshift.ocp-bluebank.io/console/command-line)
2. Run `./deploy/runme.sh`

Your service will be deployed to [https://openshift.ocp-bluebank.io/console/project/${artifactId}-server](https://openshift.ocp-bluebank.io/console/project/${artifactId}-server)
