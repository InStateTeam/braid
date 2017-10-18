# Hermes

## Context

We are attempting to create a Spike skeleton where the application tiers are connected before we even begin and not 10 minutes before the presentation.
We also want to make it embarassignly easy to expose services using a wellknown protocol: JsonRPC.
Also as a separate task, we want to make it super easy to access Corda flows.


## Nexus
This project deploys artifacts to the Nexus3 repository. To consume these modules 

### Setting up Local Maven to use Nexus3

Download <a href="https://gitlab.bluebank.io/em-tech/hermes/raw/master/maven/settings.xml" download>settings.xml</a> to `~/.m2/settings.xml`


### Setting up NPM with Nexus3

`npm config set registry http://nexus-emtech.bluebank.io/repository/npm-bluebank-group/`

