/*
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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