# Braid

[![pipeline status](https://gitlab.bluebank.io/em-tech/braid/badges/master/pipeline.svg)](https://gitlab.bluebank.io/em-tech/braid/commits/master)

![logo](art/logo-small.png)

## Context

Writing web-services appear easy at first. However, to create production ready services, REST has drawbacks:

* By default, REST frameworks present several security vulnerabilities that need to be carefully plugged e.g. XSS, CORS, JWT handling, implementation leaks in error responses, invalid content-type handling, etc.
* There is a mismatch between programmatic concepts (such as Types, Functions, Methods etc) to REST and HTTP - with far too great an emphasis on the semantics of HTTP return codes, content types etc.
* REST isn't suited to reactive, event-driven concepts - often the notion of event streams has to be manually encoded over XHR POST requests or websockets, with all the security issues that this work can often expose.
* Finally, typically, the authentication and authorisation models are intricate and require careful work to integrate with incumbent security services.

All of the above slow down project delivery, and distract focus from the value of the service being implemented.

> The `Braid` project is an answer to the above.

## Structure

There are three sets of key modules:

* [`braid-core`](braid-core) and [`braid-client-js`](braid-client-js) - these provide the core functionality of Braid. Transport, encodings, security. Server-side and javascript client side. 
* [`braid-corda`](braid-corda) - a library that makes it easy for developers to integrate into Corda providing a secure, object-oriented way of accessing Corda core and custom services using JS, F#, Python and Java. Braid is reactive, which means that aside from classic request-response invocations (eg. REST), it can stream results, making it easy to write applications that respond to transactions (eg. SockJS).
* [`braid-server`](braid-server) - a module that helps create standalone servers typically for spikes. It's not required for Corda cordapps. See [docs](braid-server/README.md) for purpose and reasons.

There are also a set of example projects:

* [`example-cordapp`](example-cordapp) - an example Corda cordapp, with customised authentication, streaming
* [`example-server`](example-server) - a simple standalone server 

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

## Building locally

You will need:

* Maven 3.5.x
* Node JS. We use 9.3.0 together with 5.6.0.


The build for all modules (Kotlin, Javascript, Cordapps etc) is orchestrated with maven.

To compile, test and install the dependencies:

```bash
mvn clean install
```

The project can be loaded into any IDE that can read Maven POMs.

## Publishing / Deploying Artifacts

```bash 
mvn clean deploy
cd client
npm install
npm run build
npm publish
```



