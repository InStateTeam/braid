'use strict';

const JsonRPC = require('./hermes-json-rpc-client');

class HermesServiceProxy {
  constructor(url, onOpen, onClose, onError, options) {
    if (!options) {
      options = {}
    }
    options.noCredentials = true;
    this.client = new JsonRPC(url, options);
    this.client.onOpen = onOpen;
    this.client.onClose = onClose;
    this.client.onError = onError;
  }

  uri() {
    const uri = document.createElement('a');
    uri.href = path
    const base = "http://" + uri.hostname + ":" + uri.port
    const serviceName = uri.pathname.split("/").filter(i => i.length > 0).pop()
    if (serviceName !== undefined && serviceName !== null) {
      return base + "/?service=" + serviceName
    } else {
      return base;
    }
  }

  invoke(method, args) {
    const callbacks = HermesServiceProxy.bindCallbacks(args);
    if (callbacks) {
      return this.invokeForStream(method, args, callbacks);
    } else {
      return this.invokeForPromise(method, args);
    }
  }

  invokeForPromise(method, args) {
    args = HermesServiceProxy.massageArgs(args);
    const thisObj = this;
    return this.client.invoke(method, args).then(result => result, err => {
      thisObj.onErrorTrap(err)
    });
  }

  invokeForStream(method, args, callbacks) {
    const noFunctionArgs = HermesServiceProxy.massageArgs(args.filter(item => {
      return typeof item !== 'function'
    }));
    return this.client.invokeForStream(method, noFunctionArgs,
      callbacks.onNext,
      this.onErrorWrapper(callbacks.onError),
      callbacks.onCompleted);
  }

  onErrorWrapper(onError) {
    const thisObj = this;
    return (err) => {
      try {
        thisObj.onErrorTrap(err)
      } catch(err2) {
        if (onError) {
          onError(err2);
        } else {
          console.error(err2);
        }
      }
    }
  }

  onErrorTrap(err) {
    if (err.code === -32601) {
      throw Error(err.message + "\nCreate a stub here: " + this.uri())
    } else {
      throw err;
    }
  }

  static massageArgs(args) {
    if (args != null) {
      if (args.length === 0) {
        args = null;
      } else if (args.length === 1) {
        args = args[0];
      }
    }
    return args;
  }

  static bindCallbacks(args) {
    if (!args) return null;
    const last3Fns = args.slice(-3).filter(item => {
      return typeof item === 'function'
    });
    if (last3Fns.length === 0) return null;
    return {
      onNext: last3Fns[0],
      onError: last3Fns[1],
      onCompleted: last3Fns[2]
    }
  }
}

function createProxy(url, onOpen, onClose, options) {
  return new Proxy(new HermesServiceProxy(url, onOpen, onClose, options), {
    get: (target, propKey, receiver) => {
      return function (...args) {
        return target.invoke(propKey, args)
      }
    }
  });
}

module.exports = createProxy