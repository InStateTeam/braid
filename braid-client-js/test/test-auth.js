import assert from 'assert'
import {buildProxy} from './util';

/**
 * Tests for authentication
 */
describe('braid authentication', () => {

  it('if we do not login, we fail', (done) => {
    buildProxy(done, proxy => {
      proxy.add(5, 6)
        .then(() => {
          throw new Error("should have raised a not authenticated error");
        })
        .catch(err => {
          // console.log(err);
          if (typeof(err.jsonRPCError) !== 'undefined') { // this is a rpc error
            assert.equal(-32000, err.jsonRPCError.code)
            assert.ok(err.jsonRPCError.message.includes('not authenticated'))
          } else {
            throw err;
          }
        })
        .then(done, done)
    })
  }).timeout(0);

  it('if we provide the wrong credentials, login fails', (done) => {
    buildProxy(done, proxy => {
      proxy.login({ username: 'admin', password: 'wrongpassword'})
        .then(() => {
          throw new Error("should have raised a not authenticated error");
        })
        .catch(err => {
          // console.log(err);
          if (typeof(err.jsonRPCError) !== 'undefined') { // this is a rpc error
            assert.equal(-32000, err.jsonRPCError.code)
            assert.ok(err.jsonRPCError.message.includes('failed to authenticate'))
          } else {
            throw err;
          }
        })
        .then(done, done)
    })
  }).timeout(0);
});
