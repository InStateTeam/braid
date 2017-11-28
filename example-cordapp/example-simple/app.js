'use strict';
import CordaProxy from 'hermes-corda-client';

console.log('demo started');

export const corda = new CordaProxy({
    url: 'https://localhost:8080/api/',
    credentials: {
        username: 'banka',
        password: 'password'
    }
}, onOpen, onClose, onError);

let notary;

function onOpen() {
    console.log('opened')
    printMyInfo(corda)
    .then(() => getNotaries())
    .then(() => registerForCashNotifications())
    .then(() => issueCash('$100', 'ref01'))
    .then(() => issueCash('$200', 'ref02'))
    .then(() => console.log('finished'), err => console.error('failed', err));
}

function printMyInfo() {
    return corda.network.myNodeInfo()
    .then(ni => {
        console.log('network info for node: ', ni);
    });
}

function getNotaries() {
    return corda.network.notaryIdentities()
    .then(notaries => {
        console.log('notaries', notaries);
        notary = notaries[0];        
    })
}

function registerForCashNotifications() {
    console.log('registered for cash notifications')    
    return corda.myService.listenForCashUpdates(onCashNotification)
}

function onCashNotification(update) {
    console.log('cash notification:', update);
}

function issueCash(amount, ref) {
    console.log('issuing', amount)
    return corda.flows.issueCash(amount, ref, notary)
    .then(result => console.log('txid for ref', ref, result), err => console.error(err));
}

function onClose() {
    console.log('closed');
}

function onError(e) {
    console.error('failed with error;', e);
}

