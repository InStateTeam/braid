"use strict";

function buildURLClass() {
  if (typeof(document) !== 'undefined' && typeof(window) !== 'undefined') {
    return BrowserURL;
  } else {
    const { URL } =require('url');
    return URL;
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

const url = buildURLClass();
console.log("url is", url);
export default url;