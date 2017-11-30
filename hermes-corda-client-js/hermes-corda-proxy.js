'use strict';

import DynamicProxy from './dynamic-service-proxy';

class CordaProxy {
  constructor(config, onOpen, onClose, onError, options) {
    if (!config.url) {
      throw "config must include url property e.g. https://localhost:8080"
    }
    const that = this;
    let errors = 0;
    let connections = 0;
    let requiredConnections = 0;

    // --- PRIVATE FUNCTIONS ---

    function onInternalOpen() {
      if (++connections === requiredConnections && errors === 0 && onOpen) {
        onOpen()
      }
    }

    function failed(e) {
      if (onError) {
        onError(e)
      } else {
        console.error(e)
      }
    }

    function onInternalClose() {
      if (connections <= 0 && errors === 0 && onClose) {
        onClose()
      }
    }

    function onInternalError() {
      if (++errors === 1 && onError) {
        onError()
      }
    }

    function bootstrap() {
      const url = config.url + "doc/";
      const oReq = new XMLHttpRequest();
      oReq.addEventListener('load', () => {
        if (oReq.status === 200) {
          bindServices(oReq);
        } else {
          clearCredentials();
          failed("failed to get metadata at " + url + ": " + oReq.statusText);
        }
      });
      oReq.addEventListener('error', failed);
      oReq.open('GET', url);
      oReq.send();
    }

    function bindServices(oReq) {
      const serviceNames = JSON.parse(oReq.response);
      requiredConnections = serviceNames.length;
      for (let idx = 0; idx < serviceNames.length; ++idx) {
        const serviceName = serviceNames[idx];
        that[serviceName] = new DynamicProxy(config, serviceName, onInternalOpen, onInternalClose, onInternalError, options);
      }
      clearCredentials();
    }

    function clearCredentials() {
      config.credentials = null;
    }

    // --- PUBLIC FUNCTIONS ---

    // --- INITIALISATION ---
    bootstrap();
  }


}

module.exports = CordaProxy;