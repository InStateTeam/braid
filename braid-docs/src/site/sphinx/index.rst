#####
Braid
#####

.. toctree::
   :maxdepth: 2
   :caption: Contents:

   concepts.rst

Introduction
============

.. image:: images/logo-small.png

*Braid is a high performance reactive rpc library for exposing your services in a secure way, with very little code, and to consume them in any language, runtime, and platform.*

**Easy integration**

Easy integration with Corda, from any:

* programming language: currently Javascript. Python, F#, and more planned
* runtime i.e. you don't need Jython, Nashorn etc.
* platform: currently browser and NodeJS. iOS and more planned

No fiddling with HTTP REST response codes, content-types, errors etc.
Just *simple object-oriented code*.

**Secure**

* Connections are secured with TLS/SSL
* Signed payloads
* Easy to integrate with Enterprise authentication and authorisation services

**Works with the Enterprise**

Multiple transports:

* HTTP XHR requests (long polls etc)
* Websockets

All using the power of `SockJS https://github.com/sockjs`

Multiple encodings:

* JSON
* Protobufs (to be introduced soon)

**Reactive**

* Basic request + response
* Streamed responses
* Backpressure (to be introduced soon)

Context
-------

Writing web-services appears easy at first. But to create production ready services, REST has drawbacks:

* By default, REST frameworks present several security vulnerabilities that need to be carefully plugged e.g. XSS, CORS, JWT handling, implementation leaks in error responses, invalid content-type handling, etc.
* There is a mismatch between programmatic concepts (such as Types, Functions etc) to REST and HTTP - with far too great an emphasis on the semantics of HTTP return codes, content types etc.
* REST isn't suited to reactive, event-driven concepts - often the notion of event streams has to be manually encoded over XHR POST requests or websockets, with all the security issues that this work can often expose.
* Finally, typically, the authentication and authorisation models are intricate and require careful work to integrate with incumbent security services.

REST slows down project delivery, adds risk, and distracts focus from the value of the service being implemented.


.. container:: codeset

   .. literalinclude:: ../../../../braid-corda/src/test/kotlin/io/bluebank/braid/corda/integration/cordapp/TestBraidCordaService.kt
      :language: kotlin
      :start-after: DOCSTART 1
      :end-before: DOCEND 1


