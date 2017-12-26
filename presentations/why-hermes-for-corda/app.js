'use strict';
import CordaProxy from 'braid-corda-client';

export let corda = null;
let notary;

function bindUI() {
  $('#btnStartDemo').onclick = startDemo;
}

function startDemo() {
  const msg = 'demo started';
  log(msg);
  console.log(msg);

  corda = new CordaProxy({
    url: 'https://localhost:8080/api/',
    credentials: {
      username: 'banka',
      password: 'password'
    }
  }, onOpen, onClose, onError);
}

function onOpen() {
  const msg = 'opened';
  console.log(msg);
  log(msg);
  printMyInfo(corda)
    .then(() => getNotaries())
    .then(() => registerForCashNotifications())
    .then(() => issueCash('$100', 'ref01'))
    .then(() => issueCash('$200', 'ref02'))
    .then(() => console.log('finished'), err => console.error('failed', err));
}

function printMyInfo() {
  const msg = 'retrieving myNodeInfo'
  console.log(msg);
  log(msg);
  return corda.network.myNodeInfo()
    .then(ni => printNodeInfo(ni));
}

function getNotaries() {
  const msg = 'retrieving notaries';
  console.log(msg);
  log(msg);
  return corda.network.notaryIdentities()
    .then(notaries => {
      notary = notaries[0];
      printNotary(notary)
    })
}

function registerForCashNotifications() {
  const msg = 'registered for cash notifications'
  console.log(msg);
  log(msg);
  return corda.myService.listenForCashUpdates(onCashNotification)
}

function onCashNotification(update) {
  console.log('cash notification:', update);
  const txid = update.produced[0].ref.txhash;
  const amount = update.produced[0].state.data.amount;
  amount.token = "...";
  const amountTxt = JSON.stringify(amount, null, 1);
  log(`notification:<br> txid: <span class="demo-output-property">${txid}</span>` +
    `<br>amount:<span class="demo-output-property">${amountTxt}</span>`)
}

function issueCash(amount, ref) {
  console.log(`issuing: ${amount} with reference ${ref}`);
  log(`issuing: <span class="demo-output-property">${amount}</span> with reference <span class="demo-output-property">${ref}</span>`);
  return corda.flows.issueCash(amount, ref, notary)
    .then(result => printSignedTx(ref, result), console.error);
}

function printNodeInfo(ni) {
  console.log('network info for node: ', ni);
  log('node info: ' +
    `<span class="demo-output-property">${ni.addresses[0].host}:${ni.addresses[0].port}</span> ` +
    `<span class="demo-output-property">${ni.legalIdentities[0].name}</span>`);
}

function printNotary(notary) {
  console.log('notary', notary);
  log(`notary: <span class="demo-output-property">${notary.name}</span>`);
}

function printSignedTx(ref, result) {
  console.log('tx for ref', ref, result);
  log(`txid for reference <span class="demo-output-property">${ref}</span>: <span class="demo-output-property">${result.stx.transaction.id}</span> `)
}

function onClose() {
  console.log('closed');
}

function onError(e) {
  console.error('failed with error;', e);
}

let logIndex = function(start, pad) {
  return function () {
    return (1e15 + start++ + "").slice(-pad);
  }
}(0, 3);


function log(innerHTML) {
  const item = document.createElement('div');
  item.className = 'demo-output-item';
  const lineNumber = document.createElement('span');
  lineNumber.className = 'demo-output-index';
  lineNumber.innerText = logIndex();

  const line = document.createElement('span');
  line.innerHTML = innerHTML;
  item.appendChild(lineNumber);
  item.appendChild(line);
  const demoOutput = $('#demoOutput');
  demoOutput.appendChild(item);
  demoOutput.scrollTop = demoOutput.scrollHeight;
}


function $(selector) {
  return document.querySelector(selector)
}

function whenReady(fn) {
  document.addEventListener('DOMContentLoaded', fn);
}

whenReady(bindUI);
