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

'use strict';

const SockJS = require('sockjs-client');
const Promise = require('promise');
const xhr = require('request');

export default class JsonRPC {
  constructor(url, options) {
    const that = this;
    let nextId = 1;
    let state = {};
    let status = "CLOSED";
    that.onOpen = null;
    that.onClose = null;
    that.onError = null;

    // -- PRIVATE FUNCTIONS -- oh Javascript

    function checkServiceExistsAndBootstrap() {
      const infoURL = url + "/info";
      let strictSSL = true;
      if (typeof options.strictSSL !== 'undefined') {
        strictSSL = options.strictSSL;
      }
      if (!strictSSL) {
        if (typeof process !== 'undefined' && typeof process.env !== 'undefined') {
          // NOTE: rather nasty - to be used only in local dev for self-signed certificates
          process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
        }
      }
      // first we do a quick check if the service exists and response to a sockjs info request
      // we do this because sockjs-client doesn't distinguish between, the lack of a sockjs service and the webserver being up
      xhr({
        method: "get",
        uri: infoURL,
        strictSSL: strictSSL,
        rejectUnauthorized: !strictSSL,
        headers: {
          "Content-Type": "application/json"
        }
      }, function(err, resp, body) {
        if (err) {
          console.log("error!", err);
          initialCheckFailed(err)
        } else if (resp) {
          onInitialCheck(resp);
        } else {
          const message = "no error nor response!";
          console.error(message, infoURL);
          that.onError(new Error(message));
        }
      })
    }

    function initialCheckFailed(e) {
      console.error("failed: ", e);
      if (that.onError) {
        let error;
        if (e.currentTarget.status === 0) {
          error = new ErrorEvent(false, false, "connection refused")
        } else  {
          error = new ErrorEvent(false, false, "unknown error")
        }
        that.onError(error);
      } else {
        console.log('initialCheckFailed', e);
      }
    }

    function onInitialCheck(oReq) {
      if ((oReq.statusCode / 100) !== 2) {
        console.log("Failed response", oReq.statusCode, oReq.statusMessage);
        if (oReq.statusMessage.startsWith('Braid: ')) {
          logBraid(oReq.statusMessage.substring(8));
        } else {
          console.error(oReq.statusMessage)
        }
        if (that.onError) {
          that.onError(new ErrorEvent(true, oReq.status !== 404, oReq.statusMessage));
        }
        return
      }
      options.rejectUnauthorized = false;
      that.socket = new SockJS(url, null, options);
      that.socket.onopen = function (e) {
        openHandler(e);
      };
      that.socket.onclose = function (e) {
        closeHandler(e);
      };
      that.socket.onerror = function (err) {
        errorHandler(err)
      };
      that.socket.onmessage = function (e) {
        messageHandler(JSON.parse(e.data));
      }
    }

    function logBraid(msg) {
      msg = msg.split('.').map((it) => {
        return it.trim();
      }).join('.\n');
      console.log("%cBraid%c\n\n" + msg, "font-size: 24pt; font-weight: bold; color: #880017; background-color: #999;", "font-size: 14px;")
    }

    function openHandler(e) {
      status = "OPEN";
      if (that.onOpen) {
        that.onOpen(e);
      }
    }

    function closeHandler(e) {
      status = "CLOSED";
      if (that.onClose) {
        that.onClose(e);
      }
    }

    function errorHandler(err) {
      that.status = "FAILED";
      if (that.onError) {
        that.onError(err);
      }
    }

    function messageHandler(message) {
      if (message.hasOwnProperty('id')) {
        if (state.hasOwnProperty(message.id)) {
          if (message.hasOwnProperty("error")) {
            handleError(message);
          } else {
            handleResponse(message);
          }
        } else {
          console.error("couldn't find callback for message identifier " + message.id);
        }
      } else {
        console.warn("received message does not have an identifier", message)
      }
    }

    function handleError(message) {
      const msgState = state[message.id];
      if (msgState.onError) {
        const err = new Error(`json rpc error ${message.error.code} with message: ${message.error.message}`);
        err.jsonRPCError = message.error;
        msgState.onError(err);
      }
      delete state[message.id];
    }

    function handleResponse(message) {
      const hasResult = message.hasOwnProperty('result');
      const isCompleted = message.hasOwnProperty('completed');
      if (hasResult) {
        handleResultMessage(message);
      }
      if (isCompleted) {
        handleCompletionMessage(message);
      }
      if (!hasResult && !isCompleted) {
        handleUnrecognisedResponseMessage(message);
      }
    }

    function handleResultMessage(message) {
      const msgState = state[message.id];
      if (!msgState) {
        console.error("could not find state for method " + message.id);
        return
      }
      if (msgState.onNext) {
        msgState.onNext(message.result);
      }
    }

    function handleCompletionMessage(message) {
      const msgState = state[message.id];
      if (msgState.onCompleted) {
        msgState.onCompleted();
      }
      delete state[message.id];
    }

    function handleUnrecognisedResponseMessage(message) {
      console.error("unrecognised json rpc payload", message);
    }

    // -- PUBLIC FUNCTIONS --

    that.invoke = function(method, params) {
      return new Promise(function (resolve, reject) {
        that.invokeForStream(method, params, resolve, reject, undefined, false);
      });
    }

    that.invokeForStream = function(method, params, onNext, onError, onCompleted, streamed) {
      const id = nextId++;
      if (streamed === undefined) {
        streamed = true;
      }

      const payload = {
        id: id,
        jsonrpc: "2.0",
        method: method,
        params: params,
        streamed: streamed
      };
      state[id] = {onNext: onNext, onError: onError, onCompleted: onCompleted};
      that.socket.send(JSON.stringify(payload));
      return new CancellableInvocation(this, id);
    }

    // -- INITIALISATION --

    checkServiceExistsAndBootstrap()
  }
}

class CancellableInvocation {
  constructor(jsonRPC, id) {
    this.jsonRPC = jsonRPC;
    this.id = id;
  }

  cancel() {
    if (this.jsonRPC.state[id]) {
      const payload = {
        cancel: this.id
      };
      this.jsonRPC.socket.send(payload);
      delete this.jsonRPC.state[id];
    }
  }
}

class ErrorEvent {
  constructor(serverFound, serviceFound, message, data) {
    this.serverFound = serverFound;
    this.serviceFound = serviceFound;
    this.message = message;
    this.data = data;
  }
}