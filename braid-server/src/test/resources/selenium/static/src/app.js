import Proxy from "braid-client"

const proxy = new Proxy("https://localhost:8080/api/myservice/braid", onOpen)

function runTest(port) {
  open(`https://localhost:$port`, proxy => {
    console.log
  })
}


function open(url, onOpen, onClose, onError, options) {
  const proxy = new Proxy(url, () => {
    callback(proxy);
  }, onClose, onError, options);
}