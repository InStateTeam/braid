import {Proxy, ServiceProxy} from '../src/Proxy'

let braidRootAPIPath = getRootAPIPath();

export function buildServiceProxy(done, callback) {
  const proxy = new ServiceProxy(getBraidService('customService'), () => {
    callback(proxy)
  }, null, done, {strictSSL: false});
  return proxy
}

export function buildProxy(config, done, callback) {
  config.url = braidRootAPIPath;
  const proxy = new Proxy(config, () => {
    callback(proxy)
  }, null, done, {strictSSL: false});
  return proxy
}

function getRootAPIPath() {
  if (typeof(process.env.braid) === 'undefined') {
    return "https://localhost:8080/api/"
  } else {
    return process.env.braid;
  }
}

function getBraidService(serviceName) {
  return braidRootAPIPath + `${serviceName}/braid`;
}
