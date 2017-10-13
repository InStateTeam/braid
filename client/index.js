import Client from 'jsonrpc-websocket-client';


/**
 * Create a proxy to a jsonrpc hermes server endpoint
 * Currently the only implemented protocol is ws://
 * @param path
 * @returns {Promise.<Proxy>}
 */
export default async function (path) {
  const client = new Client(path)
  await client.open()
  const result = {}

  const uri = function () {
    const uri = document.createElement('a');
    uri.href = path
    const base = "http://" + uri.hostname + ":" + uri.port
    const serviceName = uri.pathname.split("/").filter(i => i.length > 0).pop()
    if (serviceName !== undefined && serviceName !== null) {
      return base + "/?service=" + serviceName
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
};
