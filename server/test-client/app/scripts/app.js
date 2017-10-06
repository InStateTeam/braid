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
  const calculator = await RPCProxy('ws://localhost:8080/api/calculator')
  console.log(await calculator.add(1, 2))
  console.log(await calculator.subtract(1, 2))
  console.log(await calculator.multiply(1, 2))
  console.log(await calculator.divide(10, 2))

  const accounts = await RPCProxy('ws://localhost:8080/api/accounts')
  console.log(await accounts.createAccount("fred"))
  console.log(await accounts.createAccount("jim"))
  console.log(await accounts.getAccounts())
  console.log(await accounts.updateAccount({ id: "1", name: "henry"}))
  console.log(await accounts.getAccounts())
}

export default App;
