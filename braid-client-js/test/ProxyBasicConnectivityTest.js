import assert from 'assert';
import {buildProxy} from './Utilities';

describe('braid-corda basic connectivity and method invocation', () => {
  it('connect to a server and execute simple flow', (done) => {
    buildProxy({credentials: {username: 'admin', password: 'admin'}}, done, proxy => {
      const echoParam = "Syd was here"
      proxy.flows.echo(echoParam)
        .then(result => {
          assert.ok(result.includes(echoParam))
        })
        .then(done, done)
    })
  }).timeout(0)

  it('connect to a server and get all network nodes', (done) => {
    buildProxy({credentials: {username: 'admin', password: 'admin'}}, done, proxy => {
      proxy.network.allNodes()
        .then(nodes => {
          assert.ok(nodes.length >= 0);
          for (let n in nodes) {
            const node = nodes[n];
            assert.ok(node !== null);
            assert.ok(typeof(node.legalIdentities) !== 'undefined');
            assert.ok(node.legalIdentities.length > 0);
            for (let l in node.legalIdentities) {
              const legalIdentity = node.legalIdentities[l];
              assert.ok(typeof(legalIdentity.name) !== 'undefined');
              assert.ok(typeof(legalIdentity.owningKey) !== 'undefined');
            }
          }
        })
        .then(done, done)
    })
  }).timeout(0)

});