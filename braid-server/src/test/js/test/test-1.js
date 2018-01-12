import assert from 'assert'
import Proxy from 'braid-client'

const braidService = process.env.braidService;

console.log("braid service", braidService);

describe('Proxy', () => {
  describe('constructor', () => {
    it('should connect to a server', (done) => {
      const proxy = new Proxy(braidService, () => {
        proxy.login({ username: 'admin', password: 'admin'})
          .then (() => proxy.add(5, 6))
          .then(result => assert.equal(11, result))
          .then(done, done)
      }, null, done, { strictSSL: false });
    });
  });
});

