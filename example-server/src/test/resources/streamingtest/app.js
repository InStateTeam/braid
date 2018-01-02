'use strict';

const $ = require('jquery');
const ServiceProxy = require('./braid-service-proxy');

$(document).ready(() => {
  const url = "http://localhost:8080/api";
  const app = new App(url);
});

class App {
  constructor(url) {
    const thisObj = this;
    this.service = new ServiceProxy(url, () => { thisObj.onOpen() }, () => { thisObj.onClose() });
  }

  onOpen() {
    console.log("opened")
    this.service.login({username: 'admin', password: 'admin'})
      .then(() => { console.log('login succeeded') }, (err) => console.error('login failed', err))
      .then(() => { this.service.time(this.onTime) });
  }

  onTime(time) {
    $('#time').text(time)
  }

  onClose() {
    console.log("closed");
  }
}