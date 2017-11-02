# Hermes

## Context

* Our aim is to create a Spike skeleton where the application tiers are connected before we even begin and not 10 minutes before the presentation.
* We also want to make it embarassignly easy to expose services using a wellknown protocol: JsonRPC.
* Also as a separate task, we want to make it super easy to access Corda flows.


## Structure

1. A NPM javascript module, [hermes-client](client) to make it super easy to consume services in the UI and to alter them at runtime.
2. A Java module [hermes-server](server) to host a wide selection of services.
3. An [example-server](example-server) demonstrating a set of services.
4. An `hermes-corda` module that makes it very easy to expose Corda flows as services, securely. (This is currently being worked on)

## Nexus
This project deploys artifacts to the Nexus3 repository. To consume these modules 

### Setting up Local Maven to use Nexus3

Download <a href="https://gitlab.bluebank.io/em-tech/hermes/raw/master/maven/settings.xml" download>settings.xml</a> to `~/.m2/settings.xml`


### Setting up NPM with Nexus3

Nexus exposes a unified repository for both a npm central proxy and a locally deployed repo (private to bluebank.)
The unified repo can be used to install all artifacts. Set is as follows:

```bash
npm config set registry http://nexus-emtech.bluebank.io/repository/npm-bluebank-group/
```
Followed by

```bash
npm add-user
```
... to add your credentials for the nexus npm repository.

To be able to publish modules (such as, say, `hermes-client`) add your credential to npm using:

```bash
npm add-user --registry=http://nexus-emtech.bluebank.io/repository/npm-local/
```

This will prompt you for your credentials and email. Once successfully authorised, you should be able to publish the 
`hermes-client` module in the [client](client) directory.

## Publishing / Deploying Artifacts

```bash 
mvn clean deploy
cd client
npm install
npm run build
npm publish
```



