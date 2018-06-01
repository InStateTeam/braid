![image](../art/logo-small.png)

# Braid Core

1. [Invocation Protocol](#invocation-protocol)
    
    1. [Request payload extension](#request-payload-extension)
    2. [Response payload extension](#response-payload-extension)
    
2. [Type information API](#type-information-api)

## Invocation Protocol

The Braid invocation protocol is a backwards compatible extension to [JsonRPC 2.0](http://www.jsonrpc.org/specification).
This extension is to facilitate the treatment of streamed replies to a given request. 
The extension applies to the [Request payload](http://www.jsonrpc.org/specification#request_object) and the 
[Response payload](http://www.jsonrpc.org/specification#response_object).

### Request payload extension
 
[JsonRPC 2.0](http://www.jsonrpc.org/specification#request_object) defines the following standard fields: `jsonrpc`, `method`, `params`, and `id`.
Braid adds one addition field:

**streamed** - this is an **optional** `boolean`, with the default being `false`. If `true`, this client is expecting a streamed response. 
When `true`, the Braid server will send responses that meet the **Response** payload extension.


### Response payload extension

[JsonRPC 2.0](http://www.jsonrpc.org/specification#response_object) defines the following standard fields: `jsonrpc`, `result`, `error`, and `id`.
In addition, the Braid extension specifies one additional field:

**completed** - this is an **optional** field indicating if this response is the last result package for request with the same `id`. 
The presence of the field is sufficient to terminate the flow.

### Stream semantics

Braid's stream semantics follow that of [RX](http://reactivex.io/) Observer pattern. Given a streamed invocation, the server will continue to send packets __without the flag__ 
whilst there are more items in the stream. The stream is terminated with an JsonRPCv2 error packet __or__ a packet with the `completed` field set.

### Examples

Syntax:

```
--> data sent to Server
<-- data sent to Client
```
 
Call to a method `f1` that returns a stream of two results:

```
--> {"jsonrpc": "2.0", "method": "f1", "params": [], "id": 1, "streamed": true}
<-- {"jsonrpc": "2.0", "result": 1, "id": 1}
<-- {"jsonrpc": "2.0", "result": 2, "id": 1, "completed": true}
```

Call to method `f2` that returns only one result (but with streaming enabled):

```
--> {"jsonrpc": "2.0", "method": "f2", "params": [], "id": 2, streamed: true}
<-- {"jsonrpc": "2.0", "result": 1, "id": 2, "completed": true}
```

Call to method `f3` that returns no results (but with streaming enabled):

```
--> {"jsonrpc": "2.0", "method": "f3", "params": [], "id": 3, streamed: true}
<-- {"jsonrpc": "2.0", "id": 3, "completed": true}
```

Call to method `f4` that return two items, before terminating with an error:

```
--> {"jsonrpc": "2.0", "method": "f4", "params": [], "id": 4, streamed: true}
<-- {"jsonrpc": "2.0", "result": 1, "id": 4}
<-- {"jsonrpc": "2.0", "result": 2, "id": 4}
<-- {"jsonrpc": "2.0", "error": {"code": -32000, "message": "failure in stream"}, "id": 4}
```

## Type information API

Braid provides a simple REST API to retrieve the type information for services, their methods, and invocation binding.
While there are conventions, it is advised that the REST API is navigated to correctly bind to a service.

### API web root

By convention, the web root of the API is located at `https://<host>:<port>/api`.

### API

**GET /**

Returns a JSON object of the form

```json
{ 
  "<service-name>": {
    "endpoint": "<sock-js-url>",
    "documentation": "<documentation-url>"
  },
  ...
}
```

#### Services definition example

As an example, here is a [cordite](http://cordite.foundation/) node's end-point:

```json
{
  "network": {
    "endpoint": "/api/network/braid",
    "documentation": "/api/network"
  },
  "flows": {
    "endpoint": "/api/flows/braid",
    "documentation": "/api/flows"
  },
  "ledger": {
    "endpoint": "/api/ledger/braid",
    "documentation": "/api/ledger"
  },
  "dao": {
    "endpoint": "/api/dao/braid",
    "documentation": "/api/dao"
  },
  "meterer": {
    "endpoint": "/api/meterer/braid",
    "documentation": "/api/meterer"
  }
}
```

**GET <endpoint-url> and the sockjs protocol**

Each `endpoint` URL resolves to a [SockJS](https://github.com/sockjs/sockjs-protocol) compliant server.
You can use any one of the many SockJS clients to bind to this end point.
Typically you should be able to connect to the websocket API of the SockJS endpoint on `<endpoint-url>/websocket`.
This shouldn't be relied upon though in general - socketjs gracefully degrades from websocket connections to simple HTTPS POST
polls. This may happen either due to the way the server is configured, or any restrictions on the servers firewall rules. 


**GET <documentation-url>**

The `documentation` URL resolves to a JSON document of the form:

```json
[
  {
    "name": "<method-name>",
    "description": "<human-readable-text>",
    "parameters": {
      "<parameter-name>": "<json-schema-type-descriptor>",
      ...
    },
    "returnType": "<json-schema-type-descriptor>"
  }
]
```

Type descriptors are in the [JsonSchema](http://json-schema.org/) format.

> The way Braid uses JsonSchema is fairly naive. JsonSchema doesn't have a formalised means of providing named types. 
> The intention is to create a second type protocol that uses a richer data structure language e.g. Protobufs.

### Streamed results

In the case where a method returns a streamed response, the `returnType` schema definition is prepended with the characters `stream-of `.

### Service documentation example

For example, the `ledger` service in a [Cordite node](https://emea.edge.cordite.foundation:8080/api/ledger) generates the following:

```json
[
  {
    "name": "setAccountTag",
    "description": "",
    "parameters": {
      "accountId": "string",
      "tag": "{category:string,value:string}",
      "notary": "{commonName:string,organisationUnit:string,organisation:string,locality:string,state:string,country:string,x500Principal:{name:string,encoded:array}}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "removeAccountTag",
    "description": "",
    "parameters": {
      "accountId": "string",
      "category": "string",
      "notary": "{commonName:string,organisationUnit:string,organisation:string,locality:string,state:string,country:string,x500Principal:{name:string,encoded:array}}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "findAccountsByTag",
    "description": "",
    "parameters": {
      "tag": "{category:string,value:string}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "getAccount",
    "description": "",
    "parameters": {
      "accountId": "string"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "listAccounts",
    "description": "",
    "parameters": {},
    "returnType": "{complete:boolean}"
  },
  {
    "name": "listAccountsPaged",
    "description": "",
    "parameters": {
      "paging": "{pageNumber:integer,pageSize:integer,default:boolean}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "issueToken",
    "description": "",
    "parameters": {
      "accountId": "string",
      "amount": "string",
      "symbol": "string",
      "description": "string",
      "notary": "{commonName:string,organisationUnit:string,organisation:string,locality:string,state:string,country:string,x500Principal:{name:string,encoded:array}}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "balanceForAccount",
    "description": "",
    "parameters": {
      "accountId": "string"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "transferToken",
    "description": "",
    "parameters": {
      "amount": "string",
      "tokenTypeUri": "string",
      "fromAccount": "string",
      "toAccount": "string",
      "description": "string",
      "notary": "{commonName:string,organisationUnit:string,organisation:string,locality:string,state:string,country:string,x500Principal:{name:string,encoded:array}}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "scheduleEvent",
    "description": "",
    "parameters": {
      "clientId": "string",
      "payload": "{map:{},empty:boolean}",
      "iso8601DateTime": "{year:integer,month:string,nano:integer,monthValue:integer,dayOfMonth:integer,hour:integer,minute:integer,second:integer,dayOfYear:integer,dayOfWeek:string,chronology:{calendarType:string,id:string}}",
      "notary": "{commonName:string,organisationUnit:string,organisation:string,locality:string,state:string,country:string,x500Principal:{name:string,encoded:array}}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "listenForScheduledEvents",
    "description": "",
    "parameters": {
      "clientId": "string"
    },
    "returnType": "stream-of any"
  },
  {
    "name": "transactionsForAccount",
    "description": "",
    "parameters": {
      "accountId": "string",
      "paging": "{pageNumber:integer,pageSize:integer,default:boolean}"
    },
    "returnType": "array"
  },
  {
    "name": "listenForTransactions",
    "description": "",
    "parameters": {
      "accountIds": "array"
    },
    "returnType": "stream-of any"
  },
  {
    "name": "wellKnownTagCategories",
    "description": "",
    "parameters": {},
    "returnType": "array"
  },
  {
    "name": "wellKnownTagValues",
    "description": "",
    "parameters": {},
    "returnType": "array"
  },
  {
    "name": "createTokenType",
    "description": "",
    "parameters": {
      "symbol": "string",
      "exponent": "integer",
      "notary": "{commonName:string,organisationUnit:string,organisation:string,locality:string,state:string,country:string,x500Principal:{name:string,encoded:array}}"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "listTokenTypes",
    "description": "",
    "parameters": {
      "page": "integer",
      "pageSize": "integer"
    },
    "returnType": "{complete:boolean}"
  },
  {
    "name": "listTokenTypes",
    "description": "",
    "parameters": {},
    "returnType": "{complete:boolean}"
  },
  {
    "name": "createAccount",
    "description": "",
    "parameters": {
      "accountId": "string",
      "notary": "{commonName:string,organisationUnit:string,organisation:string,locality:string,state:string,country:string,x500Principal:{name:string,encoded:array}}"
    },
    "returnType": "{complete:boolean}"
  }
]
```
