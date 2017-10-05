export default class JSONRPC {

  constructor(path, protocol = "protocolOne") {
    this.socket = new WebSocket(path, protocol)
  }
}