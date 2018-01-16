'use strict';

import ServiceProxy from './ServiceProxy';
import URL from './UrlParse';
import xhr from 'request';

/**
 * This class creates a first-class JS object with methods that proxy to a given service end-point
 */
export default class DynamicProxy {
  /**
   *
   * @param config - an object with one required field 'url' and an optional field 'credentials' carrying the payload for the authentication service used by the server
   *  example: {
   *    url: 'https://localhost:8080/api',
   *    credentials: {
   *      username: 'foo',
   *      password: 'bar'
   *    }
   *  }
   * @param serviceName - the name of the service being bound to
   * @param onOpen - callback when this proxy has connected to the service
   * @param onClose - callback when this proxy has disconnected from the service
   * @param onError - callback when this proxy has failed to connect
   * @param options - transport level options. see SockJS options: https://github.com/sockjs/sockjs-client
   */
  constructor(config, serviceName, onOpen, onClose, onError, options) {
    const that = this;
    if (!config.url) {
      throw "missing url property in config";
    }

    let strictSSL = true;
    if (options === null) {
      options = {};
    }

    if (typeof options.strictSSL !== 'undefined') {
      strictSSL = options.strictSSL;
    }
    if (!strictSSL) {
      if (typeof process !== 'undefined' && typeof process.env !== 'undefined') {
        // NOTE: rather nasty - to be used only in local dev for self-signed certificates
        process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
      }
    }

    config = Object.assign({}, config); // clone so that we can then get rid of the credentials once logged in
    const serviceEndPoint = config.url + serviceName + "/braid";
    const proxy = new ServiceProxy(serviceEndPoint, internalOnOpen, onClose, err => failed(`to open service proxy for ${serviceName}`, err), options);

    // --- PRIVATE FUNCTIONS ---

    function internalOnOpen() {
      Promise.resolve()
        .then(() => {
          if (config.credentials) {
            return proxy.login(config.credentials)
          }
          return null;
        })
        .then(() => {
          clearCredentials();
          retrieveMetadataAndBind();
        }, err => {
          failed("failed to open", err)
        });
    }

    function retrieveMetadataAndBind() {
      try {
        const url = getMetadataEndpoint(config, serviceName);

        xhr({
          method: "get",
          uri: url,
          strictSSL: strictSSL,
          rejectUnauthorized: !strictSSL,
          headers: {
            "Content-Type": "application/json"
          }
        }, function (err, resp, body) {
          if (err) {
            failed(err);
          }
          else if (resp.statusCode !== 200) {
            failed(resp.statusMessage)
          } else if (body) {
            bindMetadata(body);
          } else {
            const message = "no error nor response!";
            console.error(message, url);
            failed(message);
          }
        });
      } catch (err) {
        failed(err);
      }
    }

    function bindMetadata(body) {
      try {
        const metadata = JSON.parse(body);
        for (let idx = 0; idx < metadata.length; ++idx) {
          bind(metadata[idx]);
        }
        if (onOpen) {
          onOpen()
        }
      } catch (error) {
        failed(error);
      }
    }

    function bind(item) {
      const name = item.name;
      if (!that.hasOwnProperty(name)) {
        const fn = function (...args) {
          return invoke(name, ...args)
        };
        fn.__metadata = []; // to populate with metadata on signature and documentation
        fn.docs = function () { printDocs(fn); };
        that[name] = fn;
      }
      // append the metadata for the method. N.B. methods can have overloads, hence the use of an array.
      const fn = that[name];
      fn.__metadata.push(item);
      return that[name];
    }

    function invoke(methodName, ...args) {
      return proxy[methodName](...args);
    }

    function failed(reason, e) {
      if (onError) {
        onError({ reason: reason, error: e });
      } else {
        console.error(reason, e);
      }
    }


    function getMetadataEndpoint(config, serviceName) {
      const parsed = parseURL(config.url);
      const result = parsed.protocol + "//" + parsed.hostname + ":" + parsed.port + "/api/" + serviceName;
      return result;
    }

    function parseURL(url) {
      return new URL(url)
    }

    function clearCredentials() {
      config.credentials = null;
    }

    /**
     * prints documentation of a function's metadata
     *
     * @param fn - the respective function
     */
    function printDocs(fn) {
      let msg = "API documentation\n" +
                "-----------------\n";
      for (let idx in fn.__metadata) {
        const methodDefinition = fn.__metadata[idx];
        if (!methodDefinition.returnType) {
          methodDefinition.returnType = 'unknown';
        }
        let apifn = "* " + methodDefinition.name + '(' + generateParamList(methodDefinition) + ') => ' + methodDefinition.returnType + '\n';
        apifn += methodDefinition.description + '\n';
        apifn += generateParamDocs(methodDefinition);
        msg += apifn + '\n\n';
      }
      console.log(msg);
    }

    function generateParamList(methodDefinition) {
      return Object.keys(methodDefinition.parameters).join(', ');
    }

    function generateParamDocs(methodDefinition) {
      return Object.keys(methodDefinition.parameters)
        .map(p => {
          return `  @param ${p} - ${methodDefinition.parameters[p]}`
        }).join('\n')
    }
    // --- PUBLIC FUNCTIONS ---

    // --- INITIALISATION ---
  }
}
