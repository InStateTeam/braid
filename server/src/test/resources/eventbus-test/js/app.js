"use strict";

const EventBus = require('vertx3-eventbus-client');
const $ = require('jquery');

$(document).ready(initEventBusApp);

function setupEventbus() {
  const eb = new EventBus("http://localhost:8080/eventbus");
  eb.defaultHeaders = {
    "Authorization": "Basic " + btoa("admin" + ":" + "admin")
  }
  eb.onopen = function () {
    onOpen(eb);
  }
  eb.onclose = function(e) {
    setTimeout(setupEventbus, 1000);
  }
}

function initEventBusApp() {
  setupEventbus();
  console.log("initialised")
}

function onOpen(eb) {
  eb.registerHandler("time", (err, message) => {
    if (err) {
      console.error(err);
    } else {
      $('#time').text(message.body)
    }
  });
}