"use strict";

const JsonRPC = require('./hermes-json-rpc-client');
const $ = require('jquery');

$(document).ready(() => {
  const url = "http://localhost:8080/api";
  const app = new App(url);
});

class App {
  constructor(url) {
    this.rpc = new JsonRPC(url);
    const thisObj = this;
    this.rpc.onOpen = () => thisObj.onOpen()
    this.rpc.onClose = () => thisObj.onClose()
  }

  onOpen() {
    console.log("opened")
    this.login();
  }

  login() {
    this.rpc.invoke("login", {username: "admin", password: "admin"})
      .then((result) => {
        console.log("login succeeded", result);
        this.logout();
      }, (error) => {
        console.log("login failed", error);
      })
  }

  logout() {
    this.rpc.invoke("logout")
      .then((result) => {
        console.log("logout succeeded", result);
      }, (error) => {
        console.log("logout failed", error);
      })
  }

  onClose() {
    console.log("closed");
  }
}