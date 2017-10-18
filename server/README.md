# Hermes Server

This server implements a dynamic JsonRPC server to demonstrate the Hermes proof of concept.
The server can expose any number of Java/Kotlin services. 
New methods can be easily added at run-time using Javascript.
Also, new services can be added at run-time, again using Javascript.

## Sample Service

Simple one line example:

```kotlin
  JsonRPCServer(rootPath = "/api/services/", port = 8080, services = listOf(CalculatorService(), AccountService())).start()
```

See this running here: [https://hermes-sample-server.bluebank.io](https://hermes-sample-server.bluebank.io)

The editor allows the UI developer to dynamically create stubbed services and service methods, overlaying existing functionality in Java services. 
Thereby, s/he can develop without being held up by the server-side.
Also the editor acts as a documentation to the server side for functions that need to be implemented.

## Building

### Maven

```bash
mvn clean install
```

### Docker + NPM

```bash
cd server
npm install
```

## Running

### Maven

```bash
mvn exec:exec
```

### Docker + NPM

```bash
npm start
```

## Ports and Paths

The server starts on port `8080`.

JsonRPC Websocket end-points are `ws://localhost:8080/api/services/<service-name>`

The editor uses the REST API:

* `GET /api/services` - retrieve list of declared services
* `GET /api/services/<service-name>` - retrieve the javascript script for the given servive
* `POST /api/services/<services-name` - POST an updated javascript body for the service.
* `GET /api/services/<service-name>/script` - retrieve Javascript stubs for the service.
* `GET /api/services/<service-name>/java` - retrieve the descriptions of the Java functions per service - for documentation

## Deployment to OpenShift

```bash
deploy/runme.sh
```