# Hermes Server

This server implements a dynamic JsonRPC server to demonstrate the Hermes proof of concept.
The server can expose any number of Java/Kotlin services. 
New methods can be easily added at run-time using Javascript.
Also, new services can be added at run-time, again using Javascript.

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

JsonRPC Websocket end-points are `ws://localhost:8080/api/<service-name>`

The editor uses the REST API:

* `GET /api/services` - retrieve list of declared services
* `GET /api/services/<service-name>` - retrieve the javascript script for the given servive
* `POST /api/services/<services-name` - POST an updated javascript body for the service.
