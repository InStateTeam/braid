const http = require('http');
const sockjs = require('sockjs');
export default function () {
  const calculator = sockjs.createServer({ sockjs_url: 'http://cdn.jsdelivr.net/sockjs/1.0.1/sockjs.min.js' });
  calculator.on('connection', function(conn) {
    console.log('onConnection');
    conn.on('data', function(message) {
      console.log('received', message);
      const response = {
        'jsonrpc': '2.0',
        'id': message.id,
        'result': 42
      };
      conn.write(response);
    });
  });
  const server = http.createServer();
  calculator.installHandlers(server, {prefix:'/api'});
  server.listen(8088, '0.0.0.0');
}