'use strict';
import CordaProxy from 'braid-corda-client';

let output;
let corda;

document.onreadystatechange = function() {
  if (document.readyState === 'complete') {
    onLoaded()
  }
}

function onLoaded() {
  $('#connection').onConnect(connect);
  output = $('#console');
}

function connect(e) {
  const url = e.detail.url;
  corda = new CordaProxy({
    url: url
  }, onOpen, onClose, onError);
}

function onOpen() {
  output.log('opened');
}

function onClose() {

}

function onError() {

}

function $(query) {
  return document.querySelector(query)
}