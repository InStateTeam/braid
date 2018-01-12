import Proxy from '..'

const braidService = process.env.braidService;

export function buildProxy(done, callback) {
  const proxy = new Proxy(braidService, () => {
    callback(proxy)
  }, null, done, { strictSSL: false} );
  return proxy
}
