'use strict';

import DynamicProxy from './dynamic-service-proxy';

const xhr = require('request');

class CordaProxy {
  constructor(config, onOpen, onClose, onError, options) {
    if (!config.url) {
      throw "config must include url property e.g. https://localhost:8080"
    }

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

    function onInternalError(e) {
      if (++errors === 1 && onError) {
        onError(e)
      }
    }

    function bootstrap() {
      const url = config.url;
      xhr({
        method: "get",
        uri: url,
        strictSSL: strictSSL,
        rejectUnauthorized: !strictSSL,
        headers: {
          "Content-Type": "application/json"
        }
      }, function(err, resp, body) {
        if (err) {
          clearCredentials();
          failed("failed to get services descriptor: " + err)
        } else if (resp) {
          bindServices(body);
        }
      })
    }

    function bindServices(body) {
      const services = JSON.parse(body)
      const serviceNames = Object.keys(services);
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