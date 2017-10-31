'use strict';

const SockJS = require('sockjs-client');
const Promise = require('promise');

class JsonRPC {
  constructor(url, options) {
    this.url = url;
    this.options = options;
    this.nextId = 1;
    this.state = {};
    this.status = "CLOSED";
    this.onOpen = null;
    this.onClose = null;
    this.onError = null;
    const thisObj = this;
    // first we do a quick check if the service exists and response to a sockjs info request
    // we do this because sockjs-client doesn't distinguish between, the lack of a sockjs service and the webserver being up
    const oReq = new XMLHttpRequest();

    oReq.addEventListener('load', () => thisObj.onInitialCheck(oReq));
    oReq.addEventListener('error', (e) => thisObj.initialCheckFailed(e));
    oReq.open('GET', url + '/info');
    oReq.send();
  }

  onInitialCheck(oReq) {
    if ((oReq.status / 100) !== 2) {
      if (oReq.statusText.startsWith('Hermes: ')) {
        JsonRPC.logHermes(oReq.statusText.substring(8));
      } else {
        console.error(oReq.statusText)
      }
      if (this.onError) {
        this.onError(new ErrorEvent(true, oReq.status !== 404, oReq.statusText))
      }
      return
    }
    const thisObj = this;
    this.socket = new SockJS(this.url, null, this.options);
    this.socket.onopen = function (e) {
      thisObj.openHandler(e);
    }
    this.socket.onclose = function (e) {
      thisObj.closeHandler(e);
    }
    this.socket.onerror = function (err) {
      thisObj.errorHandler(err)
    }
    this.socket.onmessage = function (e) {
      thisObj.messageHandler(JSON.parse(e.data));
    }
  }

  initialCheckFailed(e) {
    if (this.onError) {
      var error;
      if (e.currentTarget.status === 0) {
        error = new ErrorEvent(false, false, "connection refused")
      } else  {
        error = new ErrorEvent(false, false, "unknown error")
      }
      this.onError(error);
    } else {
      console.log('initialCheckFailed', e);
    }
  }

  static logHermes(msg) {
    msg = msg.split('.').map((it) => {
      return it.trim();
    }).join('.\n');
    console.log("%cHermes%c\n\n" + msg, "font-size: 24pt; font-weight: bold; color: #880017; background-color: #999;", "font-size: 14px;")
  }

  openHandler(e) {
    this.status = "OPEN";
    if (this.onOpen) {
      this.onOpen(e);
    }
  }

  closeHandler(e) {
    this.status = "CLOSED";
    if (this.onClose) {
      this.onClose(e);
    }
  }

  errorHandler(err) {
    this.status = "FAILED";
    if (this.onError) {
      this.onError(err);
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
      const err = new Error(`json rpc error ${message.error.code} with message ${message.error.message}`);
      err.jsonRPCError = message.error;
      state.onError(err);
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
    if (!state) {
      console.error("could not find state for method " + message.id);
      return
    }
    // console.log("received", message);
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
      thisObj.invokeForStream(method, params, resolve, reject, undefined, false);
    });
  }

  invokeForStream(method, params, onNext, onError, onCompleted, streamed) {
    const id = this.nextId++
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
    // console.log("payload", payload);
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

class ErrorEvent {
  constructor(serverFound, serviceFound, message, data) {
    this.serverFound = serverFound;
    this.serviceFound = serviceFound;
    this.message = message;
    this.data = data;
  }
}

module.exports = JsonRPC;