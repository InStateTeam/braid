'use strict';

import ServiceProxy from 'hermes-client';

class DynamicProxy {
  constructor(config, serviceName, onOpen, onClose, onError, options) {
    const thisObj = this;
    if (!config.url) {
      throw "missing url property in config";
    }
    this.__onOpen = onOpen;
    this.__config = Object.assign({}, config); // clone so that we can then get rid of the credentials once logged in
    this.__serviceName = serviceName;
    this.__onError = onError;
    this.__proxy = new ServiceProxy(this.__config.url + "jsonrpc/" + serviceName,
      thisObj._onOpen.bind(thisObj),
      onClose, onError, options);
  }

  _onOpen() {
    const thisObj = this;
    Promise.resolve()
      .then(() => {
        if (this.__config.credentials) {
          return thisObj.__proxy.login(thisObj.__config.credentials)
        }
        return null;
      })
      .then(() => {
        this._clearCredentials()
        thisObj._retrieveDocsAndBind()
      }, err => {
        thisObj._retrieveDocsAndBind()
        if (thisObj.__onError) {
          thisObj.__onError(err);
        } else {
          console.error("failed to open", err);
        }
      });
  }

  _retrieveDocsAndBind() {
    const thisObj = this;
    const url = this._getDocumentationEndpoint(this.__config, this.__serviceName);
    const oReq = new XMLHttpRequest();
    oReq.addEventListener('load', () => {
      thisObj._bindDocs(oReq);
      if (thisObj.__onOpen) {
        thisObj.__onOpen();
      }
    });
    oReq.addEventListener('error', (e) => thisObj._failed(e));
    oReq.open('GET', url);
    oReq.send();
  }

  _bindDocs(oReq) {
    if (oReq.status !== 200) {
      this._failed(oReq.status)
      return
    }
    const docs = JSON.parse(oReq.response);
    for (let idx = 0; idx < docs.length; ++idx) {
      this._bind(docs[idx]);
    }
  }

  _bind(item) {
    const fn = this._createOrReturnFunction(item);
    fn.__docs.push(item);
  }

  _createOrReturnFunction(item) {
    const name = item.name;
    if (!this.hasOwnProperty(name)) {
      const fnp = [];
      for (let paramName in item.parameters) {
        if (item.parameters.hasOwnProperty(paramName)) {
          fnp.push(paramName);
        }
      }
      fnp.push('return this.__proxy.' + name + '(...arguments)');
      const fn = new Function(...fnp);
      fn.__docs = []; // to populate with docs on calling convention
      fn.docs = _printDocs.bind(fn);
      this[name] = fn;
    }
    return this[name];
  }

  _failed(e) {
    console.error("failed to get docs", e);
  }

  _getDocumentationEndpoint(config, name) {
    const parsed = this._parseURL(config.url)
    const url = parsed.protocol + "//" + parsed.hostname + ":" + parsed.port + "/api/doc/" + name
    return url
  }

  _parseURL(url) {
    const parser = document.createElement('a');
    parser.href = url;
    return parser;
  }

  _clearCredentials() {
    this.__config.credentials = null;
  }
}

function _printDocs() {
  let msg = "API documentation\n" +
    "-----------------\n";
  for (let idx in this.__docs) {
    const defn = this.__docs[idx];
    let apifn = "* " + defn.name + '(' + _generateParamList(defn) + ') => ' + defn.returnType + '\n';
    apifn += defn.description + '\n';
    apifn += _generateParamDocs(defn);
    msg += apifn + '\n\n';
  }
  console.log(msg);
}

function _generateParamList(defn) {
  return Object.keys(defn.parameters).join(', ');
}

function _generateParamDocs(defn) {
  return Object.keys(defn.parameters)
    .map(p => {
      return `  @param ${p} - ${defn.parameters[p]}`
    }).join('\n')
}

module.exports = DynamicProxy;