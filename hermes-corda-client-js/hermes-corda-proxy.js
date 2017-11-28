'use strict';

import DynamicProxy from './dynamic-service-proxy';

class CordaProxy {
  constructor(config, onOpen, onClose, onError, options) {
    if (!config.url) {
      throw "config must include url property e.g. https://localhost:8080"
    }
    const thisObj = this;
    thisObj.__errors = 0;
    thisObj.__connections = 0;
    thisObj.__onOpen = onOpen;
    thisObj.__onClose = onClose;
    thisObj.__onError = onError;
    thisObj.__config = config;
    thisObj.__options = options;
    thisObj._bootstrap();
  }

  onOpen() {
    if (++this.__connections === this.__requiredConnections && this.__errors === 0 && this.__onOpen) {
      this.__onOpen()
    }
  }

  onClose() {
    if (--this.__connections <= 0 && this.__errors === 0 && this.__onClose) {
      this.__onClose()
    }
  }

  onError() {
    if (++this.__errors === 1 && this.__onError) {
      this.__onError()
    }
  }

  _bootstrap() {
    const thisObj = this;
    const url = thisObj.__config.url + "doc/";
    const oReq = new XMLHttpRequest();
    oReq.addEventListener('load', () => {
      if (oReq.status === 200) {
        thisObj._bindServices(oReq);
      } else {
        thisObj._clearCredentials();
        thisObj._failed("failed to get metadata at " + url + ": " + oReq.statusText);
      }
    });
    oReq.addEventListener('error', (e) => thisObj._failed(e));
    oReq.open('GET', url);
    oReq.send();
  }

  _bindServices(oReq) {
    const thisObj = this;
    const serviceNames = JSON.parse(oReq.response);
    thisObj.__requiredConnections = serviceNames.length;
    for (let idx = 0; idx < serviceNames.length; ++idx) {
      const serviceName = serviceNames[idx];
      thisObj[serviceName] = new DynamicProxy(thisObj.__config, serviceName,
        thisObj.onOpen.bind(thisObj), thisObj.onClose.bind(thisObj), thisObj.onError.bind(thisObj), thisObj.__options);
    }
    thisObj._clearCredentials()
  }

  _failed(e) {
    if (this.__onError) {
      this.__onError(e)
    } else {
      console.error(e)
    }
  }

  _clearCredentials() {
    this.__config.credentials = null;
  }
}

module.exports = CordaProxy;