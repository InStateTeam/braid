'use strict';

const $ = require('jquery');
const ServiceProxy = require('hermes-client');
// Use the following line instead of the above, to debug using the local source for hermes-client
// const ServiceProxy = require('../hermes-client-js/hermes-service-proxy');

$(document).ready(() => {
  const url = "http://localhost:8080/api/jsonrpc/time";
  const app = new App(url);
});

class App {
  constructor(url) {
    this.url = url;
    this.connect();
  }

  connect() {
    const thisObj = this;
    this.service = new ServiceProxy(this.url, (e) => { thisObj.onOpen(e) }, (e) => { thisObj.onClose(e) }, (err) => { thisObj.onError(err); });
  }

  onOpen() {
    console.log("opened")
    this.service.login({username: 'admin', password: 'admin'})
      .then(() => { console.log('login succeeded') }, (err) => console.error('login failed', err))
      .then(() => { this.service.time(this.onTime) })
    ;
  }

  onTime(time) {
    $('#time').text(time)
  }

  onClose(e) {
    const thisObj = this;
    setTimeout(() => { thisObj.connect() }, 5000);
  }

  onError(err) {
    if (!err.serverFound) {
      const thisObj = this;
      setTimeout(() => { thisObj.connect() }, 5000);
    }
  }
}