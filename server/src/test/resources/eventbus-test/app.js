"use strict";

const SockJS = require('sockjs-client');
const Promise = require('promise');
const EventBus = require('vertx3-eventbus-client');
const $ = require('jquery');

const username = "admin";
const password = "admin";


$(document).ready(() => {
  initSockJS()
});

function initSockJS() {
  const endpoint = "http://localhost:8080/api"
  const sock = new SockJS(endpoint, null, null);
  sock.onopen = () => {
    console.log("opened");
    sock.send(JSON.stringify({ str: "Hello, World"}));
    sock.send(JSON.stringify({ operation: "LOGIN", credentials: { username: "admin", password: "admin" }} ));
    sock.send(JSON.stringify({ str: "Hello, World"}));
  }
  sock.onmessage = (e) => {
    console.log(e.data);
  }
  sock.onclose = function(e) {
    console.log("closed", e);
  }
}

function JsonRPC(url, options) {
  const socket = new SockJS(url, null, options);

  const jsonRPC = {
    nextId: 1,
    state: {},
    status: "CLOSED",
    onOpen: null,
    onClose: null,
    call: doCall,
    callForStream: doCallForStream
  }

  socket.onopen = function() {
    onOpen.call(jsonRPC)
  }

  socket.onmessage = function(data) {
    onMessage.call(jsonRPC, data);
  }

  socket.onclose = function(e) {
    onClose.call(jsonRPC, e)
  }

  function onOpen() {
    this.status = "OPEN";
    if (this.onOpen) {
      this.onOpen();
    }
  }

  function onMessage(data) {
    if (data.hasOwnProperty("id")) {
      handleMessageWithId.call(this, data);
    } else {
      handleUnboundMessage.call(this, data);
    }
  }

  function onClose(e) {
    console.log("onClose", this, e);
    // clear all state
    this.state = {};
    if (this.onClose) {
      this.onClose(e)
    }
  }

  function doCall(method, params) {
    return new Promise(function(resolve, reject){
      this.doCallForStream(method, params, resolve, reject);
    });
  }

  function doCallForStream(method, params, onNext, onError, onCompleted) {
    const payload = {
      id: this.id++,
      jsonrpc: "2.0",
      method: method,
      params: params
    };

    this.state[payload.id] = { onNext: onNext, onError: onError, onCompleted: onCompleted};
    socket.send(JSON.stringify(payload));
  }

  function handleUnboundMessage(data) {
    console.log("unbound message", data);
  }

  function handleMessageWithId(data) {
    if (this.state.hasOwnProperty(data.id)) {
      if (data.hasOwnProperty("error")) {
        handleError.call(this, data);
      } else {
        handleResponse.call(this, data);
      }
    } else {
      console.error("couldn't find callback for message identifier " + data.id);
    }
  }

  function handleError(data) {

  }

  function handleResponse(data) {

  }

  function getCallbacks(id) {
    return this.state[id];
  }
  return jsonRPC;
}
