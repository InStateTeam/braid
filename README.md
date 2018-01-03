# Braid

[![pipeline status](https://gitlab.bluebank.io/em-tech/braid/badges/master/pipeline.svg)](https://gitlab.bluebank.io/em-tech/braid/commits/master)

## Context

Writing web-services appears easy at first. However, the machinery of REST frameworks often slow development, and by-default introduce security concerns that a long time to fix.
Further, traditional REST services do not cater for reactive services that enable clients to respond to changes.
Finally, typically, the authentication and authorisation models are intricate and require careful work to integrate with incumbent security services.

The `Braid` project is an answer to the above.

There are three modules:

* `braid-core` and `braid-client-js` - these provide the core functionality of Braid. Transport, encodings, security. Server-side and javascript client side. 
* `braid-corda` - a library that makes it easy for developers to integrate into Corda providing a secure, object-oriented way of accessing Corda core and custom services using JS, F#, Python and Java. Braid is reactive, which means that aside from classic request-response invocations (eg. REST), it can stream results, making it easy to write applications that respond to transactions (eg. SockJS).
* `braid-server` - a server for a unique way of creating spikes - this module is focussed on closer interaction between UI and server side developers. UI develoeprs can dynamically add and modify server functionality, whilst the server developers provide full implementations later.


## Structure

1. A NPM javascript module, [braid-client](client) to make it super easy to consume services in the UI and to alter them at runtime.
2. A Java module [braid-server](server) to host a wide selection of services.
3. An [example-server](example-server) demonstrating a set of services.
4. An `braie-corda` module that makes it very easy to expose Corda flows as services, securely. (This is currently being worked on)

## Nexus
This project deploys artifacts to the Nexus3 repository. To consume these modules 

### Setting up Local Maven to use Nexus3

Download <a href="https://gitlab.bluebank.io/em-tech/braid/raw/master/maven/settings.xml" download>settings.xml</a> to `~/.m2/settings.xml`


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

To be able to publish modules (such as, say, `braid-client`) add your credential to npm using:

```bash
npm add-user --registry=http://nexus-emtech.bluebank.io/repository/npm-local/
```

This will prompt you for your credentials and email. Once successfully authorised, you should be able to publish the 
`braid-client` module in the [client](client) directory.

## Publishing / Deploying Artifacts

```bash 
mvn clean deploy
cd client
npm install
npm run build
npm publish
```



