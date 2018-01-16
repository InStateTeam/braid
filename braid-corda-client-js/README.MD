# braid-corda-client module

## How to use the library

You can reference the library using either es6 `import`:

```javascript
import CordaProxy from 'braid-corda-client'
```

or es5 `require`:

```javascript
var CodaProxy = require('braid-corda-client')

```

## Proxy construction

Signature is:

`ProxyCorda(connectionOptions, onOpen, onClose, onError, transportOptions)`

The parameters are:

* **required** `connectionOptions` - an object of the form

```javascript
{
  url: 'https://localhost:8080/api/'
  credentials: {
    // ...
  }
}
```

The `url` is the root api path on the braid server.
The `credentials` is AN **optional** object and only required if the server requires credentials.
The payload of the object depends on the auth provider in the braid server.
For example, if using Shiro, the credentials object will look like this:

```javascript
{
  url: 'https://localhost:8080/api',
  credentials: {
    username: 'admin',
    password: 'admin'
  }
}
```

* **optional** `onOpen` - a callback with no parameters, for when the proxy is connected.
* **optional** `onClose` - a callback with no parameters, for when the proxy is disconnected.
* **optional** `onError` - a callback with one `error` parameter, when there is a connection error.
* **optional** `transportOptions` - an optional object to control the transport. 

`transportOptions` supports all [`sockjs-client`](https://github.com/sockjs/sockjs-client) options. 
In addition it has one optional parameter:

* `strictSSL` - a flag, that when `true` expects strict TLS server certificate and host DNS validation.
The default is `true`. Set this to `false` only for local development when using self-certified certs.

## Services and invocation

The proxy exposes "services" as a set of fields on the proxy.

These can be invoked in various Javascript idioms.

* as a callback:

```javascript
proxy.network.allNodes(function (nodes) {
  console.log(nodes);
})
```

or with an error handler as well:

```javascript
proxy.network.allNodes(
  function (nodes) {
    console.log(nodes);
  }, function (error) {
    console.error(error);
  })
```

* as a Promise (in this case using es6 arrow lambdas):

```javascript
proxy.network.allNodes()
.then(nodes => console.log(nodes));
```

* a reactive stream:

```javascript
proxy.network.state(
  change => console.log(change), 
  error => console.error(error), 
  () => { console.log("finished")});
```

## Core Corda Services

This proxy enables two default services: `flows` for access to exposed flows; and `network` for access to the network map.

These can be accessed directly off the proxy. e.g. `proxy.network.allNodes()`.

See [SimpleNetworkMapService](/Users/fuzz/dev/braid/braid-corda/src/main/kotlin/io/bluebank/braid/corda/services/SimpleNetworkMapService.kt)
for methods on the `network` object

The `flows` service accesses any exposed flows.

e.g.

```javascript
proxy.flows.echo("hello")
.then(response => console.log(response));
```

