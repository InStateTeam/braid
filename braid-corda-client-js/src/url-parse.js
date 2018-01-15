"use strict";

function buildURLClass() {
  if (typeof(document) !== 'undefined' && typeof(window) !== 'undefined') {
    return BrowserURL;
  } else {
    return require('url');
  }
}

class BrowserURL {
  constructor(url) {
    const parser = document.createElement('a');
    parser.href = url;
    this.protocol = parser.protocol;
    this.host = parser.hostname + ":" + parser.port;
    this.hostname = parser.hostname;
    this.port = parser.port
  }
}

export default url = buildURLClass();