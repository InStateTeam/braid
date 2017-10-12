import Client from 'jsonrpc-websocket-client';

module.exports = {
  RPCProxy: async function (path, servicename) {
    const client = new Client(path)
    await client.open()
    const result = {}

    const uri = function () {
      const uri = document.createElement('a');
      uri.href = path
      const base = "http://" + uri.hostname + ":" + uri.port
      if (servicename !== undefined && servicename !== null) {
        return base + "/?service=" + servicename
      } else {
        return base;
      }
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
          }
        )
        }
      }
    });
  }
}