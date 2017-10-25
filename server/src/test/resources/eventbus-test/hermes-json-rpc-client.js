"use strict";

const SockJS = require('sockjs-client');
const Promise = require('promise');
import Rx from 'rxjs/Rx';

class JsonRPC {
  constructor(url, options) {
    this.url = url;
    this.options = options;
    this.nextId = 1;
    this.state = {};
    this.status = "CLOSED";
    this.onOpen = null;
    this.onClose = null;
    this.socket = new SockJS(this.url, null, this.options);
    const thisObj = this;
    this.socket.onopen = function () {
      thisObj.openHandler();
    }
    this.socket.onclose = function () {
      thisObj.closeHandler();
    }
    this.socket.onmessage = function (e) {
      console.log("received", e.data);
      thisObj.messageHandler(JSON.parse(e.data));
    }
  }

  openHandler() {
    this.status = "OPEN";
    if (this.onOpen) {
      this.onOpen();
    }
  }

  closeHandler() {
    this.status = "CLOSED";
    if (this.onClose) {
      this.onClose();
    }
  }

  messageHandler(message) {
    if (message.hasOwnProperty('id')) {
      if (this.state.hasOwnProperty(message.id)) {
        if (message.hasOwnProperty("error")) {
          this.handleError(message);
        } else {
          this.handleResponse(message);
        }
      } else {
        console.error("couldn't find callback for message identifier " + message.id);
      }
    } else {
      console.warn("received message does not have an identifier", message)
    }
  }

  handleError(message) {
    const state = this.state[message.id];
    if (state.onError) {
      state.onError(new Error(`json rpc error ${message.error.code} with message ${message.error.message}`));
    }
    delete this.state[message.id];
  }

  handleResponse(message) {
    const hasResult = message.hasOwnProperty('result');
    const isCompleted = message.hasOwnProperty('completed');
    if (hasResult) {
      this.handleResultMessage(message);
    }
    if (isCompleted) {
      this.handleCompletionMessage(message);
    }
    if (!hasResult && !isCompleted) {
      this.handleUnrecognisedResponseMessage(message);
    }
  }

  handleResultMessage(message) {
    const state = this.state[message.id];
    if (state.onNext) {
      state.onNext(message.result);
    }
  }

  handleCompletionMessage(message) {
    const state = this.state[message.id];
    if (state.onCompleted) {
      state.onCompleted();
    }
    delete this.state[message.id];
  }

  handleUnrecognisedResponseMessage(message) {
    console.error("unrecognised json rpc payload", message);
  }

  invoke(method, params) {
    const thisObj = this;
    return new Promise(function (resolve, reject) {
      thisObj.invokeForStream(method, params, resolve, reject);
    });
  }

  invokeForStream(method, params, onNext, onError, onCompleted) {
    const id = this.nextId++

    const payload = {
      id: id,
      jsonrpc: "2.0",
      method: method,
      params: params
    };

    this.state[id] = {onNext: onNext, onError: onError, onCompleted: onCompleted};
    this.socket.send(JSON.stringify(payload));
    return new CancellableInvocation(this, id);
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
      }
      this.jsonRPC.socket.send(payload);
      delete this.jsonRPC.state[id];
    }
  }
}

module.exports = JsonRPC;