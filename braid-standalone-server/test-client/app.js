const {Proxy} = require('braid-client');

const proxy = new Proxy({url: "http://localhost:8080"}, onOpen, onClose, onError);

async function onOpen() {
  console.log('opened')
  const result = await proxy.myservice.echo('message: hello!');
  console.log(result);
}

function onClose() {
  console.log('connection closed');
}

function onError(err) {
  console.error(err);
}