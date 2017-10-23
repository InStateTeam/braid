"use strict";

const JsonRPC = require('hermes-json-rpc-client');
const $ = require('jquery');

$(document).ready(() => {
  const url = "http://localhost:8080/api";
  const app = new App(url);
});

class App {
  constructor(url) {
    this.rpc = JsonRPC(url);
    const thisObj = this;
    this.rpc.onOpen = () => thisObj.onOpen()
    this.rpc.onClose = () => thisObj.onClose()
  }
  onOpen() {
    console.log("opened")
  }
  onClose() {
    console.log("closed");
  }
}


