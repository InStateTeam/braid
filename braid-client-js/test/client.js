import 'babel-register'
import 'babel-polyfill'

const ServiceProxy = require('../');

var calculator = null
export default async function App () {
  console.log("starting client");
  calculator = ServiceProxy('http://localhost:8088/api', onOpen);
}

function onOpen() {
  console.log("onOpen");
  calculator.add(1, 2).then( result => { console.log(result) });
}
