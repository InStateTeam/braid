# Example App - Display Server Time

This app connects to the [sample app](../server/src/main/kotlin/io/bluebank/jsonrpc/sample/App.kt) 
directory.

# Build

Dependencies are installed using
```bash
npm  install
```

The source can be built with:
```bash 
npm run build
```

A continuous build can be run with:

```bash
npm run watch 
```

To run a continous build and a webserver, on [http://localhost:8081](http://localhost:8081):

```bash
npm run watch-server
```

The build uses the **deployed** `hermes-client` library.

To make it use the local source, edit the `app.js` file to point to the respective relative directories in [client](../client).



