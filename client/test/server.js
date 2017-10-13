const WebSocketServer = require('rpc-websockets').Server;

export default function () {
  // instantiate Server and start listening for requests
  const server = new WebSocketServer({
    port: 8088,
    host: 'localhost'
  });

  const service = server.of("/api/calculator");

  // register an RPC method
  service.register('add', function (params) {
    return params[0] + params[1]
  })

  // create an event
  // server.event('feedUpdated')

  // get events
  // console.log(server.eventList())

  // emit an event to subscribers
  // server.emit('feedUpdated')

  // close the server
  //server.close()
}