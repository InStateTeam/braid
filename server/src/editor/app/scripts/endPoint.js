export default function getEndPoint(serviceName){
  const host = window.location.host;
  const service = serviceName;
  document.querySelector('#host').innerHTML = "ws://" + host;
  document.querySelector('#calls').innerHTML = "/api/services/" + service;

}