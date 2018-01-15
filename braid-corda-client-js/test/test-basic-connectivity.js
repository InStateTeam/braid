import assert from 'assert';
import {buildProxy} from './util';

describe('braid-corda basic connectivity and method invocation', () => {
  it('connect to a server and execute simple flow', (done) => {
    buildProxy({ credentials: { username: 'admin', password: 'admin' } }, done, proxy => {
      console.log("logged in!");
      const echoParam = "Syd was here"
      proxy.flows.echo(echoParam)
        .then(result => {
          assert.ok(result.includes(echoParam))
        })
        .then(done, done)
    })
  }).timeout(0)
});