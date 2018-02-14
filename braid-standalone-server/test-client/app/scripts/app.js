/*
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  const calculator = await RPCProxy('ws://localhost:8080/api/services/calculator')
  console.log(await calculator.add(1, 2))
  console.log(await calculator.subtract(1, 2))
  console.log(await calculator.multiply(1, 2))
  console.log(await calculator.divide(10, 2))
  console.log(await calculator.exp(2, 3))

  const accounts = await RPCProxy('ws://localhost:8080/api/services/accounts')
  console.log(await accounts.createAccount("fred"))
  console.log(await accounts.createAccount("jim"))
  console.log(await accounts.getAccounts())
  console.log(await accounts.updateAccount({ id: "1", name: "henry"}))
  console.log(await accounts.getAccounts())
}

export default App;
