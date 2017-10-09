import Client from 'jsonrpc-websocket-client';

async function App () {
  const client = new Client('ws://127.0.0.1:8080');

  console.log(client.status)
  // → closed

  await client.open()

  console.log(client.status)
  // → open

  console.log(
    await client.call('dave', [1, 2])
  )

  await client.close()
}

export default App;