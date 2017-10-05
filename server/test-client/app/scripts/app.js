import Client from 'jsonrpc-websocket-client';

async function RPCProxy(path) {
  const client = new Client(path)
  await client.open()
  const result = {}

  const uri = function() {
    const uri = document.createElement('a');
    uri.href = path
    return "http://" + uri.hostname + ":" + uri.port
  }

  return new Proxy(result, {
    get: function (target, propKey, receiver) {
      if (propKey === "then") {
        return client.then
      }
      return function (...args) {
        return client.call(propKey, args).then(result => result, err => {
          if (err.code === -32601) {
            throw Error(err.message + "\nCreate a stub here: " + uri())
          } else {
            throw err
          }
        })
      }
    }
  });
}

async function App () {
  const proxy = await RPCProxy('ws://localhost:8080/api/calculator')
  console.log(await proxy.add(1, 2))
  console.log(await proxy.subtract(1, 2))
  console.log(await proxy.multiply(1, 2))
  console.log(await proxy.divide(10, 2))
}

export default App;
