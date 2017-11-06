export default function getEndPoint(serviceName) {
  const control = document.querySelector('#calls');
  if (!serviceName || serviceName.trim() === "") {
    control.value = ""
  } else {
    const host = window.location.host;
    control.value = window.location.protocol + "//" + host + "/api/jsonrpc/" + serviceName;
    let inputWidth = control.value.length * 8.5;
    control.style.width = inputWidth + 'px';
  }
}