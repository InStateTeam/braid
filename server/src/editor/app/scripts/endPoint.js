export default function getEndPoint(serviceName){
  const host = window.location.host;
  const service = serviceName;
  document.querySelector('#host').innerHTML = "http://" + host;
  document.querySelector('#calls').value = "/api/jsonrpc/" + service;

}