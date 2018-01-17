/*
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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