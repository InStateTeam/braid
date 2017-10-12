'use strict';

var _jsonrpcWebsocketClient = require('jsonrpc-websocket-client');

var _jsonrpcWebsocketClient2 = _interopRequireDefault(_jsonrpcWebsocketClient);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _asyncToGenerator(fn) { return function () { var gen = fn.apply(this, arguments); return new Promise(function (resolve, reject) { function step(key, arg) { try { var info = gen[key](arg); var value = info.value; } catch (error) { reject(error); return; } if (info.done) { resolve(value); } else { return Promise.resolve(value).then(function (value) { step("next", value); }, function (err) { step("throw", err); }); } } return step("next"); }); }; }

module.exports = {
  RPCProxy: function () {
    var _ref = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee(path, servicename) {
      var client, result, uri;
      return regeneratorRuntime.wrap(function _callee$(_context) {
        while (1) {
          switch (_context.prev = _context.next) {
            case 0:
              client = new _jsonrpcWebsocketClient2.default(path);
              _context.next = 3;
              return client.open();

            case 3:
              result = {};

              uri = function uri() {
                var uri = document.createElement('a');
                uri.href = path;
                var base = "http://" + uri.hostname + ":" + uri.port;
                if (servicename !== undefined && servicename !== null) {
                  return base + "/?service=" + servicename;
                } else {
                  return base;
                }
              };

              return _context.abrupt('return', new Proxy(result, {
                get: function get(target, propKey, receiver) {
                  if (propKey === "then") {
                    return client.then;
                  }
                  return function () {
                    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
                      args[_key] = arguments[_key];
                    }

                    return client.call(propKey, args).then(function (result) {
                      return result;
                    }, function (err) {
                      if (err.code === -32601) {
                        throw Error(err.message + "\nCreate a stub here: " + uri());
                      } else {
                        throw err;
                      }
                    });
                  };
                }
              }));

            case 6:
            case 'end':
              return _context.stop();
          }
        }
      }, _callee, this);
    }));

    function RPCProxy(_x, _x2) {
      return _ref.apply(this, arguments);
    }

    return RPCProxy;
  }()
};