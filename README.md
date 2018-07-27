# Braid

[![pipeline status](https://gitlab.com/bluebank/braid/badges/master/pipeline.svg)](https://gitlab.com/bluebank/braid/commits/master)

![logo](braid-docs/src/site/sphinx/images/logo-small.png) 

## Contents

1. [Introduction](#introduction) 
2. [Context](#context)
    * [Containers](#containers)
    * [Consuming Services](#consuming-services)
    * [Protocol](#protocol)   
3. [Building locally](#building-locally)
4. [Publishing / Deploying Artifacts](#publishing-/-deploying-artifacts)
 
## Introduction

_Braid is a high performance reactive rpc library for exposing your services in a secure way, with very little code, and to consume them in any language, runtime, and platform._

**Easy integration**

Easy integration with Corda, from any:
 
* programming language: currently Javascript. Python, F#, and more planned
* runtime i.e. you don't need Jython, Nashorn etc.  
* platform: currently browser and NodeJS. iOS and more planned

No fiddling with HTTP REST response codes, content-types, errors etc.
Just _simple object-oriented code_.

**Secure**

* Connections are secured with TLS/SSL
* Signed payloads
* Easy to integrate with Enterprise authentication and authorisation services

**Works with the Enterprise**

Multiple transports: 

* HTTP XHR requests (long polls etc)
* Websockets

All using the power of [SockJS](https://github.com/sockjs)

Multiple encodings:

* JSON
* Protobufs (to be introduced soon)

**Reactive**

* Basic request + response
* Streamed responses
* Backpressure (to be introduced soon)
 
## Context

Writing web-services appears easy at first. But to create production ready services, REST has drawbacks:

* By default, REST frameworks present several security vulnerabilities that need to be carefully plugged e.g. XSS, CORS, JWT handling, implementation leaks in error responses, invalid content-type handling, etc.
* There is a mismatch between programmatic concepts (such as Types, Functions etc) to REST and HTTP - with far too great an emphasis on the semantics of HTTP return codes, content types etc.
* REST isn't suited to reactive, event-driven concepts - often the notion of event streams has to be manually encoded over XHR POST requests or websockets, with all the security issues that this work can often expose.
* Finally, typically, the authentication and authorisation models are intricate and require careful work to integrate with incumbent security services.

REST slows down project delivery, adds risk, and distracts focus from the value of the service being implemented. 

**Braid is an answer to the above**

* Expose your services as plain old Java/Kotlin/Scala classes.
* Your methods can return any type including Futures and reactive streams.
* Currently, consume your services in Javascript. Plans are in place for Python, C#, F#, GoLang and more.
* Braid makes good assumptions on security, serialisation, and transports - which you can alter if you wish.
* Uses open protocols for its transports and serialisation: Websockets, HTTP.
* It's fast - capable of handling 8-9K concurrent requests per second on a small Amazon instance; and it scales well.
* Your services will be exposed with rich metadata (far better than Swagger) that will give you a rich developer experience on the client side.
* You can plugin your preferred authentication services.

### Containers

There are various containers that Braid can work in:
 
* [Standalone processes](braid-standalone-server)
* [Corda nodes](braid-corda)

Both of these rely on [braid-core](braid-core).

### Consuming Services 

Services can be consumed in the following languages and runtimes (more to follow!)

* [javascript](braid-client-js) in a browser or NodeJS
* [any JVM language](braid-client)

### Protocol

If you want to implement your own client, or are just interested in how Braid works, the protocol is defined **[here](./braid-core/README.md)**.

## Examples

* [`example-cordapp`](examples/example-cordapp) - an example Corda cordapp, with customised authentication, streaming
* [`example-server`](examples/example-server) - a simple standalone server 
* [another cordapp](https://github.com/joeldudleyr3/pigtail) - another example by [Joel Dudley](https://twitter.com/joeldudley6)


## Building locally

You will need:

* Maven 3.5.x
* Node JS. We use 9.3.0 together with NPM 5.6.0.

The build for all modules (Kotlin, Javascript, Cordapps etc) is orchestrated with maven.

To compile, test and install the dependencies:

```bash
mvn clean install
```

The project can be loaded into any IDE that can read Maven POMs.

## Publishing / Deploying Artifacts

1. Checkout master
2. Make sure you pull the latest master
3. Use maven versions plugin to bump the version to the next snapshot

```
mvn versions:set -DnewVersion=3.0.3-SNAPSHOT
```

4. Git add and commit 
5. Don’t push yet (to speed up the process)
6. Git create a new branch off master with the correct release version e.g. v3.0.2

```
git checkout -b v3.0.2
```

7. Push this release branch (ie not the master branch)
8. In gitlab CI there is a manual job for the branch that you will kick off
9. Then checkout master and push it
10. Log into [https://oss.sonatype.org/](https://oss.sonatype.org/)
11. Go to the `Staging Repositories` tab, and search for `iobluebank`, locating the current staged release.
12. Close the release and release it.

Obviously the above should be automated with a script - ideally integrated worth maven

