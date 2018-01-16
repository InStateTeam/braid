import CordaProxy from '..'

let braidRootAPIPath = getRootAPIPath();

export function buildProxy(config, done, callback) {
  config.url = braidRootAPIPath;
  const proxy = new CordaProxy(config, () => { callback(proxy) }, null, done, { strictSSL: false} );
  return proxy
}

function getRootAPIPath() {
  if (typeof(process.env.braid) === 'undefined') {
    return "https://localhost:8081/api/"
  } else {
    return process.env.braid;
  }
}