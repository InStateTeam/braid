# braid-client NPM module

## Short Example

```bash
npm install --save braid-client
```

followed by:

```javascript

const calculator = new ServiceProxy("http://localhost:8080/api/jsonrpc/calculator", onOpen);
function onOpen() {
  calculator.add(1, 2)
  .then((result) => { console.log(result); })
}
```

## Features

Allows for the calling of any service method exposed by the end point.
If the service method is not available, the module emits a message and URL to the developer console.
Clicking on the URL takes the UI developer to the Braid Editor to add the new method the service.

## Build

```npm run build```

