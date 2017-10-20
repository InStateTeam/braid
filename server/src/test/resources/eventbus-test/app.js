"use strict";

const SockJS = require('sockjs-client');
const EventBus = require('vertx3-eventbus-client');
const $ = require('jquery');

const username = "admin";
const password = "admin";


$(document).ready(() => {
  // initSockJS()
  initEventBusApp();
});

function initSockJS() {
  const endpoint = "http://localhost:8080/api"
  const sock = new SockJS(url, null, options);
  sock.onopen = () => {
    console.log("opened");
  }
  sock.onmessage = (e) => {
    console.log(e.data);
  }
  sock.onclose = function(e) {
    console.log("closed");
  }
}

function JsonRPCProxy(url, options) {
  options = options || {}
  const sockJS = new SockJS(url, null, options)
  return {
    call: function(method, args) {
      const envelope = {

      }
    }
  }
}

function getAuthHeaders() {
  return {
    "Authorization": "Basic " + btoa(username + ":" + password)
  };
}

function setupEventbus() {
  const eb = new EventBus("http://localhost:8080/eventbus");
  eb.defaultHeaders = getAuthHeaders()
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
      console.error(message.body);
      $('#time').text(message.body);
    }
  });
}