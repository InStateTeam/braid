import assert from 'assert';
import {buildServiceProxy} from './Utilities';

describe('ServiceProxyBasicConnectivityTest', () => {

  it('connect to a server, login, and call a simple method', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.login({username: 'admin', password: 'admin'})
        .then(() => proxy.add(5, 6))
        .then(result => assert.equal(11, result))
        .then(done, done)
    })
  }).timeout(0);

  it('that a method that throws is reported in the client', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.login({username: 'admin', password: 'admin'})
        .then(() => proxy.badjuju())
        .then(() => {
          throw new Error("method should have raised an error");
        })
        .catch(err => {
          // console.log(err);
          if (typeof(err.jsonRPCError) !== 'undefined') { // this is a rpc error
            assert.equal(-32000, err.jsonRPCError.code)
            assert.equal("I threw an exception", err.jsonRPCError.message)
          } else {
            throw err;
          }
        })
        .then(done, done)
    });
  }).timeout(0);

  it('that an unknown method raises an appropriate exception', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.login({username: 'admin', password: 'admin'})
        .then(() => proxy.unknownMethod())
        .then(() => {
          throw new Error("method should have raised an error");
        })
        .catch(err => {
          // console.log(err);
          if (typeof(err.jsonRPCError) !== 'undefined') { // this is a rpc error
            assert.equal(-32601	, err.jsonRPCError.code)
            assert.ok(err.jsonRPCError.message.includes('unknownMethod'))
          } else {
            throw err;
          }
        })
        .then(done, done)
    });
  }).timeout(0);
});




