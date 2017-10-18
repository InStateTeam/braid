# hermes-client NPM module

## Short Example

```bash
npm install --save hermes-client
```

followed by:

```javascript
const calculator = await RPCProxy('ws://localhost:8088/api/calculator')
  console.log(await calculator.add(1, 2))
```

## Features

Allows for the calling of any service method exposed by the end point.
If the service method is not available, the module emits a message and URL to the developer console.
Clicking on the URL takes the UI developer to the Hermes Editor to add the new method the service.

## Build

```npm run build```

## Deploy / Publish to Nexus

```npm run deploy```

You will be ask for the username, password and email. I used:

```bash
user: admin
password: 8a5500n!
email: fuzz@bluebank.io # it can be anything
```
